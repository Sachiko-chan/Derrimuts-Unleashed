import test from 'node:test';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {root,run,tool} from '../scripts/common.mjs';
test('gym codes reject unsafe URLs and correlate only fresh matching scans', () => {
    const dir=fs.mkdtempSync(path.join(os.tmpdir(),'gyms-test-'));
    try {
        run('java',['-jar',tool('ecj.jar'),'-source','8','-target','8','-encoding','UTF-8','-d',dir,path.join(root,'java/privacy/GymCodes.java'),path.join(root,'tests/GymCodesTest.java')]);
        run('java',['-cp',dir,'GymCodesTest']);
    } finally { fs.rmSync(dir,{recursive:true,force:true}); }
});
