import test from 'node:test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {root, run, tool} from '../scripts/common.mjs';

test('gym codes reject unsafe URLs and correlate only fresh matching scans', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'gyms-test-'));
    try {
        run('java', ['-jar', tool('ecj.jar'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-d', dir,
            path.join(root, 'java/privacy/GymCodes.java'),
            path.join(root, 'tests/GymCodesTest.java')]);
        run('java', ['-cp', dir, 'GymCodesTest']);
    } finally { fs.rmSync(dir, {recursive: true, force: true}); }
});

test('saved gym selection resolves to Android-compatible lat/lon payloads only for known gyms', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'saved-gyms-loc-test-'));
    try {
        const android = tool('android.jar');
        const json = tool('json.jar');
        run('java', ['-jar', tool('ecj.jar'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', android + path.delimiter + json,
            '-d', dir,
            path.join(root, 'java/privacy/GymCodes.java'),
            path.join(root, 'java/privacy/CheckInTrace.java'),
            path.join(root, 'java/privacy/SavedGyms.java'),
            path.join(root, 'tests/SavedGymsLocationTest.java')]);
        run('java', ['-cp', dir + path.delimiter + json, 'SavedGymsLocationTest']);
    } finally { fs.rmSync(dir, {recursive: true, force: true}); }
});
