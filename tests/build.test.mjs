import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {readZip, writeZip} from '../scripts/zip.mjs';
import {root} from '../scripts/common.mjs';

test('deterministic archives, compression, Unicode and Android alignment', () => {
  const items = new Map([
    ['res/こんにちは.xml', {data: Buffer.from('<hello/>'), method: 8}],
    ['classes.dex', {data: Buffer.from('dex bytes'), method: 0}],
    ['lib/arm64-v8a/test.so', {data: Buffer.alloc(177, 42), method: 0}]
  ]);
  const a = writeZip(items), b = writeZip(new Map([...items].reverse()));
  assert(a.equals(b));
  const decoded = readZip(a);
  for (const [name, e] of items) assert(decoded.get(name).data.equals(e.data));
  assert.equal(decoded.get('classes.dex').offset % 4, 0);
  assert.equal(decoded.get('lib/arm64-v8a/test.so').offset % 16384, 0);
});
test('corruption is rejected before packaging', () => {
  const bytes = writeZip(new Map([['classes.dex', {data: Buffer.from('abcdef'), method: 0}]]));
  const offset = readZip(bytes).get('classes.dex').offset;
  bytes[offset] ^= 1;
  assert.throws(() => readZip(bytes), /CRC mismatch/);
});
test('truncated and missing ZIP directories are rejected', () => {
  assert.throws(() => readZip(Buffer.alloc(10)), /Truncated ZIP/);
  assert.throws(() => readZip(Buffer.alloc(80000)), /Missing ZIP directory/);
});
test('unsupported original APKs fail before downloading tools', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'unleashed-input-'));
  try {
    const file = path.join(dir, 'input.zip');
    fs.writeFileSync(file, writeZip(new Map([['base.apk', {data: Buffer.from('wrong version'), method: 0}]])));
    const r = spawnSync(process.execPath, [path.join(root, 'build.mjs'), file], {encoding: 'utf8'});
    assert.notEqual(r.status, 0);
    assert.match(r.stderr, /Unsupported or changed input: base.apk/);
  } finally { fs.rmSync(dir, {recursive: true, force: true}); }
});
