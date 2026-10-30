import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {root, lock, hash, run, tool} from './scripts/common.mjs';
import {readZip, writeZip} from './scripts/zip.mjs';
import {patch, walk} from './scripts/patch.mjs';
import {compileGyms, withoutObserverHooks} from './scripts/gyms.mjs';

const args = process.argv.slice(2), input = args.find(a => !a.startsWith('--'));
assert(input, 'Usage: node build.mjs ORIGINAL.zip [--repro-check]');
assert(args.every(a => a === input || a === '--repro-check'), 'Unknown argument');
const inputZip = readZip(fs.readFileSync(path.resolve(input))), sources = new Map();
for (const [name, checksum] of Object.entries(lock.apks)) {
  const data = inputZip.get(name)?.data;
  assert(data, `Missing ${name} in input ZIP`);
  assert.equal(hash(data), checksum, `Unsupported or changed input: ${name}`);
  sources.set(name, data);
}
const apktool = tool('apktool.jar'), editor = tool('APKEditor.jar');
const buildDir = path.join(root, '.work'), out = path.join(root, 'output');
fs.mkdirSync(out, {recursive: true});
const java = (jar, params) => run('java', ['-jar', jar, ...params]);
const protectedPatterns = [
  '/com/innovatise/accessControl/', '/com/innovatise/home/NFCReader.smali',
  '/gf/a.smali', '/jg/d.smali'
];
function buildOnce(round) {
  const dir = path.join(buildDir, round);
  fs.rmSync(dir, {recursive: true, force: true}); fs.mkdirSync(dir, {recursive: true});
  const inputs = path.join(dir, 'inputs'), decoded = path.join(dir, 'decoded'), frameworks = path.join(dir, 'framework');
  fs.mkdirSync(inputs);
  for (const [name, bytes] of sources) fs.writeFileSync(path.join(inputs, name), bytes);
  const originalMerged = path.join(dir, 'original.apk');
  java(editor, ['m', '-i', inputs, '-o', originalMerged]);
  // Decode code + manifest. Rebuilt resources are deliberately discarded below.
  java(apktool, ['d', '-f', '-p', frameworks, '-o', decoded, path.join(inputs, 'base.apk')]);
  const protectedFiles = walk(decoded).filter(f => protectedPatterns.some(p => f.includes(p)));
  assert(protectedFiles.length >= 8, 'Access-control class set unexpectedly changed');
  const protectedHashes = new Map(protectedFiles.map(f => [f, hash(fs.readFileSync(f))]));
  const patches = patch(decoded);
  for (const [f, expected] of protectedHashes) assert.equal(hash(withoutObserverHooks(fs.readFileSync(f, 'utf8'))), expected, `Changed gate/auth code beyond observer hooks: ${f}`);
  const rebuilt = path.join(dir, 'rebuilt.apk');
  java(apktool, ['b', '-p', frameworks, '-o', rebuilt, decoded]);
  fs.copyFileSync(rebuilt, path.join(inputs, 'base.apk'));
  const patchedMerged = path.join(dir, 'patched.apk');
  java(editor, ['m', '-i', inputs, '-o', patchedMerged]);
  const original = readZip(fs.readFileSync(originalMerged)), changed = readZip(fs.readFileSync(patchedMerged));
  const final = new Map(original);
  assert(!final.has('classes3.dex'), 'Unexpected third dex in upstream app');
  final.set('classes3.dex', {data: compileGyms(dir), method: 0});
  for (const name of ['AndroidManifest.xml', 'classes.dex', 'classes2.dex']) {
    assert(changed.has(name)); final.set(name, {data: changed.get(name).data, method: name.endsWith('.dex') ? 0 : 8});
  }
  for (const name of final.keys()) {
    if (/^META-INF\/.*(?:\.SF|\.RSA|\.DSA|\.EC|MANIFEST\.MF)$/i.test(name)) final.delete(name);
  }
  const result = writeZip(final), check = readZip(result);
  let resourceCount = 0, nativeCount = 0;
  for (const [name, entry] of original) {
    if (name.startsWith('res/') || name === 'resources.arsc' || name.endsWith('.so')) {
      assert(check.get(name)?.data.equals(entry.data), `Original resource/native bytes changed: ${name}`);
      if (name.endsWith('.so')) nativeCount++; else resourceCount++;
    }
  }
  for (const [name, entry] of check) {
    if (entry.method === 0) assert.equal(entry.offset % (name.endsWith('.so') ? 16384 : 4), 0, `Unaligned ${name}`);
    assert(!/\.(?:p12|jks|keystore)$/.test(name), 'Private key in APK');
  }
  fs.writeFileSync(path.join(out, 'patch-log.json'), JSON.stringify(patches, null, 2) + '\n');
  return {result, checks: {originalResourcesPreserved: resourceCount, nativeLibrariesPreserved: nativeCount,
    gateAndAuthClassesUnchangedApartFromObserverHooks: protectedHashes.size, zipCrcsVerified: check.size, alignment: '4-byte; native libraries 16 KiB'}};
}
const first = buildOnce('one');
let reproduced = false;
if (args.includes('--repro-check')) {
  const second = buildOnce('two');
  assert(first.result.equals(second.result), `Reproduction mismatch: ${hash(first.result)} / ${hash(second.result)}`);
  reproduced = true;
}
const name = 'Derriumuts-Unleashed-110.60-arm64-r13-unsigned.apk', digest = hash(first.result);
fs.writeFileSync(path.join(out, name), first.result);
fs.writeFileSync(path.join(out, 'SHA256SUMS'), `${digest}  ${name}\n`);
const javaVersion = run('java', ['-version'], {stdio: 'pipe'}).stderr.toString().trim();
fs.writeFileSync(path.join(out, 'build-report.json'), JSON.stringify({revision: 'r13', sha256: digest,
  cleanBuildsCompared: reproduced ? 2 : 1, unsignedByteIdentical: reproduced,
  node: process.version, java: javaVersion, inputs: lock.apks, tools: lock.tools,
  checks: first.checks, runtimeTested: false}, null, 2) + '\n');
console.log(`Built ${name}\nSHA256 ${digest}\nRuntime/gate testing is still required.`);
