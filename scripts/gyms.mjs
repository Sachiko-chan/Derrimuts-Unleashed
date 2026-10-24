import fs from 'node:fs';
import path from 'node:path';
import {root, run, tool} from './common.mjs';

export function compileGyms(dir) {
    const classes = path.join(dir, 'gym-classes'), dex = path.join(dir, 'gym-dex');
    fs.mkdirSync(classes); fs.mkdirSync(dex);
    const android = tool('android.jar');
    const sources = ['GymCodes', 'SavedGyms', 'CheckInTrace'].map(n => path.join(root, 'java/privacy', n + '.java'));
    const json = tool('json.jar');
    run('java', ['-jar', tool('ecj.jar'), '-source', '8', '-target', '8', '-encoding', 'UTF-8', '-g:none', '-classpath', android + path.delimiter + json, '-d', classes, ...sources]);
    const inputs = fs.readdirSync(path.join(classes, 'privacy')).sort().map(n => path.join(classes, 'privacy', n));
    run('java', ['-cp', tool('r8.jar'), 'com.android.tools.r8.D8', '--release', '--min-api', '26', '--lib', android, '--lib', json, '--output', dex, ...inputs]);
    return fs.readFileSync(path.join(dex, 'classes.dex'));
}

// These additions observe request identity and success. They change no auth branches.
export const bindHook = '    invoke-static {v2, p1}, Lprivacy/SavedGyms;->bind(Ljava/lang/Object;Lorg/json/JSONObject;)V\n';
export const successHook = '    iget-object v0, p0, Lcom/innovatise/accessControl/b;->a:Lgf/a;\n    invoke-static {v1, v0}, Lprivacy/SavedGyms;->success(Landroid/app/Activity;Ljava/lang/Object;)V\n';
export const installHook = '    invoke-static {p0}, Lprivacy/SavedGyms;->install(Landroid/app/Activity;)V\n';
export function withoutObserverHooks(text) { return text.replace(bindHook, '').replace(successHook, '').replace(installHook, ''); }
