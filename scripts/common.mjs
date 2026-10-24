import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import assert from 'node:assert/strict';
export const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const lock = JSON.parse(fs.readFileSync(path.join(root, 'build-lock.json')));
export const hash = data => createHash('sha256').update(data).digest('hex');
export function run(command, args, options = {}) {
  const result = spawnSync(command, args, {cwd: root, stdio: 'inherit', ...options});
  if (result.error) throw result.error;
  assert.equal(result.status, 0, `${command} failed`);
  return result;
}
export function tool(name) {
  const spec = lock.tools[name]; assert(spec, `Unknown tool ${name}`);
  const file = path.join(root, '.cache', name); fs.mkdirSync(path.dirname(file), {recursive: true});
  if (!fs.existsSync(file)) {
    run('curl', ['--fail', '--location', '--silent', '--show-error', '--retry', '2', '--max-time', '180', '-o', file + '.tmp', spec.url]);
    assert.equal(hash(fs.readFileSync(file + '.tmp')), spec.sha256, `Downloaded tool checksum: ${name}`);
    fs.renameSync(file + '.tmp', file);
  }
  assert.equal(hash(fs.readFileSync(file)), spec.sha256, `Cached tool checksum: ${name}`);
  return file;
}
