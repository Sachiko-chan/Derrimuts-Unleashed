import {inflateRawSync, deflateRawSync} from 'node:zlib';
import assert from 'node:assert/strict';

export function readZip(buf) {
  assert(buf.length >= 22, 'Truncated ZIP');
  const minimum = Math.max(0, buf.length - 65557);
  let end = buf.length - 22;
  for (; end >= minimum; end--) {
    if (buf.readUInt32LE(end) === 0x06054b50 && end + 22 + buf.readUInt16LE(end + 20) === buf.length) break;
  }
  assert(end >= minimum, 'Missing ZIP directory');
  const count = buf.readUInt16LE(end + 10);
  let pos = buf.readUInt32LE(end + 16);
  const files = new Map();
  for (let j = 0; j < count; j++) {
    assert.equal(buf.readUInt32LE(pos), 0x02014b50);
    const method = buf.readUInt16LE(pos + 10), flags = buf.readUInt16LE(pos + 8);
    assert(!(flags & 1), 'Encrypted ZIP unsupported');
    const size = buf.readUInt32LE(pos + 20), rawSize = buf.readUInt32LE(pos + 24);
    const nl = buf.readUInt16LE(pos + 28), el = buf.readUInt16LE(pos + 30), cl = buf.readUInt16LE(pos + 32);
    const offset = buf.readUInt32LE(pos + 42), name = buf.subarray(pos + 46, pos + 46 + nl).toString();
    assert(!files.has(name), `Duplicate ZIP entry: ${name}`);
    assert(!name.startsWith('/') && !name.split('/').includes('..'), 'Unsafe ZIP path');
    assert.equal(buf.readUInt32LE(offset), 0x04034b50);
    const start = offset + 30 + buf.readUInt16LE(offset + 26) + buf.readUInt16LE(offset + 28);
    const packed = buf.subarray(start, start + size);
    assert(method === 0 || method === 8, 'Unsupported ZIP compression');
    const data = method === 0 ? packed : inflateRawSync(packed);
    assert.equal(data.length, rawSize);
    assert.equal(crc32(data), buf.readUInt32LE(pos + 16), `CRC mismatch: ${name}`);
    files.set(name, {data, method, packed, offset: start});
    pos += 46 + nl + el + cl;
  }
  return files;
}
const table = Array.from({length: 256}, (_, n) => {
  for (let j = 0; j < 8; j++) n = (n & 1) ? 0xedb88320 ^ (n >>> 1) : n >>> 1;
  return n >>> 0;
});
export function crc32(data) {
  let crc = 0xffffffff;
  for (const b of data) crc = table[(crc ^ b) & 255] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

// Stable entry order, timestamps, permissions and padding. Preserve compressed
// bytes from the original where possible; never rebuild its resources.
export function writeZip(files) {
  const chunks = [], central = [];
  let offset = 0;
  for (const name of [...files.keys()].sort()) {
    if (name.endsWith('/')) continue;
    const entry = files.get(name), data = entry.data, filename = Buffer.from(name);
    const stored = name.endsWith('.so') || name.endsWith('.dex') || name === 'resources.arsc' || entry.method === 0;
    const method = stored ? 0 : 8;
    const packed = stored ? data : (entry.method === 8 && entry.packed ? entry.packed : deflateRawSync(data, {level: 9}));
    let padding = 0;
    if (stored) {
      const alignment = name.endsWith('.so') ? 16384 : 4;
      padding = (alignment - ((offset + 30 + filename.length) % alignment)) % alignment;
      if (padding && padding < 4) padding += alignment;
    }
    const extra = Buffer.alloc(padding);
    if (padding) { extra.writeUInt16LE(0xd935); extra.writeUInt16LE(padding - 4, 2); }
    const crc = crc32(data), local = Buffer.alloc(30), directory = Buffer.alloc(46);
    local.writeUInt32LE(0x04034b50); local.writeUInt16LE(20, 4); local.writeUInt16LE(0x800, 6);
    local.writeUInt16LE(method, 8); local.writeUInt16LE(0x2821, 12);
    local.writeUInt32LE(crc, 14); local.writeUInt32LE(packed.length, 18); local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(filename.length, 26); local.writeUInt16LE(extra.length, 28);
    directory.writeUInt32LE(0x02014b50); directory.writeUInt16LE(0x314, 4); directory.writeUInt16LE(20, 6);
    directory.writeUInt16LE(0x800, 8); directory.writeUInt16LE(method, 10); directory.writeUInt16LE(0x2821, 14);
    directory.writeUInt32LE(crc, 16); directory.writeUInt32LE(packed.length, 20); directory.writeUInt32LE(data.length, 24);
    directory.writeUInt16LE(filename.length, 28); directory.writeUInt32LE(0x81a40000, 38); directory.writeUInt32LE(offset, 42);
    chunks.push(local, filename, extra, packed); central.push(directory, filename);
    offset += local.length + filename.length + extra.length + packed.length;
  }
  const cd = Buffer.concat(central), end = Buffer.alloc(22), count = central.length / 2;
  end.writeUInt32LE(0x06054b50); end.writeUInt16LE(count, 8); end.writeUInt16LE(count, 10);
  end.writeUInt32LE(cd.length, 12); end.writeUInt32LE(offset, 16);
  return Buffer.concat([...chunks, cd, end]);
}
