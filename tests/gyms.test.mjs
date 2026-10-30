import test from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
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

test('saved gym metadata and compiled check-in callback signatures', () => {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'saved-gyms-loc-test-'));
    try {
        const android = tool('android.jar');
        const json = tool('json.jar');
        run('java', ['-jar', tool('ecj.jar'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-classpath', android + path.delimiter + json,
            '-d', dir,
            path.join(root, 'java/privacy/GymCodes.java'),
            path.join(root, 'java/privacy/CheckInTrace.java'),
            path.join(root, 'java/privacy/SavedGyms.java'),
            path.join(root, 'tests/HelperLinkageTest.java'),
            path.join(root, 'tests/SavedGymsLocationTest.java')]);
        run('java', ['-cp', [dir, android, json].join(path.delimiter), 'SavedGymsLocationTest']);
        const refs = [
            'Lprivacy/SavedGyms;->install(Landroid/app/Activity;)V',
            'Lprivacy/SavedGyms;->scanned(Ljava/lang/String;)V',
            'Lprivacy/SavedGyms;->bind(Ljava/lang/Object;Lorg/json/JSONObject;)V',
            'Lprivacy/SavedGyms;->success(Landroid/app/Activity;Ljava/lang/Object;)V'
        ];
        const file = path.join(dir, 'links.txt');
        fs.writeFileSync(file, refs.join('\n'));
        const cp = [dir, android, json].join(path.delimiter);
        run('java', ['-cp', cp, 'HelperLinkageTest', file]);
        // The guard must reject a reference that compiles in smali but has no Java target.
        fs.writeFileSync(file, 'Lprivacy/SavedGyms;->missingCallback(Landroid/app/Activity;)V');
        const bad = spawnSync('java', ['-cp', cp, 'HelperLinkageTest', file], {encoding: 'utf8'});
        assert.notEqual(bad.status, 0);
        assert.match(bad.stderr, /NoSuchMethodException/);

    } finally { fs.rmSync(dir, {recursive: true, force: true}); }
});
