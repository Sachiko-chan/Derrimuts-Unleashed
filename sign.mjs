import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {root, run, tool, hash} from './scripts/common.mjs';
const input = path.resolve(process.argv[2] || path.join(root, 'output/Derriumuts-Unleashed-110.60-arm64-r10-unsigned.apk'));
const output = path.resolve(process.argv[3] || input.replace('-unsigned.apk', '.apk'));
assert.notEqual(input, output, 'Output must differ from input');
const key = process.env.KEYSTORE, alias = process.env.KEY_ALIAS || 'privacy';
assert(key && process.env.KEYSTORE_PASSWORD, 'Set KEYSTORE and KEYSTORE_PASSWORD; optionally KEY_ALIAS and KEY_PASSWORD');
const dir = path.join(root, '.cache/signing'); fs.mkdirSync(dir, {recursive: true});
for (const name of ['apksigner.deb', 'apksig.deb']) run('dpkg-deb', ['-x', tool(name), dir]);
const jar = path.join(dir, 'usr/share/java/apksigner.jar');
const args = ['-jar', jar, 'sign', '--ks', path.resolve(key), '--ks-key-alias', alias,
  '--ks-pass', 'env:KEYSTORE_PASSWORD', '--v1-signing-enabled', 'false', '--v2-signing-enabled', 'true',
  '--v3-signing-enabled', 'true', '--v4-signing-enabled', 'false', '--out', output];
if (process.env.KEY_PASSWORD) args.push('--key-pass', 'env:KEY_PASSWORD');
run('java', [...args, input]);
run('java', ['-jar', jar, 'verify', '--verbose', '--print-certs', output]);
fs.writeFileSync(output + '.sha256', `${hash(fs.readFileSync(output))}  ${path.basename(output)}\n`);
console.log(`Signed ${output}`);
