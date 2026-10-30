import fs from 'node:fs';
import path from 'node:path';
import {run} from './common.mjs';

export function verifyJavaLinks(classes, decoded, android, json, reportDir) {
    const refs = new Set();
    function visit(dir) {
        for (const e of fs.readdirSync(dir, {withFileTypes: true})) {
            const file = path.join(dir, e.name);
            if (e.isDirectory()) visit(file);
            else if (e.name.endsWith('.smali')) {
                const text = fs.readFileSync(file, 'utf8');
                for (const match of text.matchAll(/invoke-static(?:\/range)?\s+\{[^}]*\},\s+(Lprivacy\/(?:SavedGyms|GymCodes|CheckInTrace);->[^\s(]+\([^)]*\)[^\s]+)/g)) refs.add(match[1]);
            }
        }
    }
    visit(decoded);
    const file = path.join(reportDir, 'helper-links.txt');
    fs.writeFileSync(file, [...refs].sort().join('\n') + '\n');
    run('java', ['-cp', [classes, android, json].join(path.delimiter), 'HelperLinkageTest', file]);
    return refs.size;
}
