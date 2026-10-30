import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import {root, hash} from './common.mjs';
import {bindHook, successHook, installHook} from './gyms.mjs';

export const removedPermissions = [
  'android.permission.AUTHENTICATE_ACCOUNTS', 'android.permission.GET_ACCOUNTS',
  'android.permission.MANAGE_ACCOUNTS', 'android.permission.READ_PHONE_STATE',
  'android.permission.WRITE_EXTERNAL_STORAGE', 'android.permission.ACCESS_COARSE_LOCATION',
  'android.permission.ACCESS_FINE_LOCATION', 'android.permission.WRITE_SETTINGS',
  'android.permission.POST_NOTIFICATIONS',
  'android.permission.READ_CALENDAR', 'android.permission.WRITE_CALENDAR',
  'android.permission.RECEIVE_BOOT_COMPLETED', 'android.permission.ACCESS_WIFI_STATE',
  'com.google.android.providers.gsf.permission.READ_GSERVICES', 'com.google.android.c2dm.permission.RECEIVE'
];
const escape = text => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
export function patch(decoded) {
  const log = [];
  function edit(rel, transform) {
    const file = path.join(decoded, rel), before = fs.readFileSync(file, 'utf8'), after = transform(before);
    assert.notEqual(after, before, `Patch made no change: ${rel}`);
    fs.writeFileSync(file, after); log.push({file: rel, before: hash(before), after: hash(after)});
  }
  function method(rel, signature, body = '    .locals 0\n    return-void') {
    edit(rel, text => {
      const re = new RegExp('^\\.method ' + escape(signature) + '\\n[\\s\\S]*?^\\.end method', 'gm');
      assert.equal([...text.matchAll(re)].length, 1, `Method mismatch: ${rel} ${signature}`);
      return text.replace(re, '.method ' + signature + '\n' + body + '\n.end method');
    });
  }
  // Passive local diagnostics: preserve all original request and authorization branches.
  function entry(rel, signature, call) {
    edit(rel, text => {
      const start = '.method ' + signature + '\n';
      assert.equal(text.split(start).length, 2);
      const at = text.indexOf(start), end = text.indexOf('.end method', at);
      const body = text.slice(at, end);
      assert.match(body, /    \.locals \d+\n/);
      return text.slice(0, at) + body.replace(/(    \.locals \d+\n)/, '$1' + call + '\n') + text.slice(end);
    });
  }
  const base = 'smali_classes2/com/innovatise/utils/w.smali';
  for (const [sig, name] of [
    ['public final J(Ljava/lang/String;)V', 'begin'],
    ['public final I(Lcom/innovatise/splash/DeepLinkInfo;)V', 'resolved'],
    ['public final G(Lcom/innovatise/module/Module;Lcom/innovatise/splash/DeepLinkInfo;Lcom/innovatise/utils/SourceInfo;Z)V', 'routed'],
    ['public final D()V', 'hidden']
  ]) entry(base, sig, '    invoke-static {p0}, Lprivacy/CheckInTrace;->' + name + '(Landroid/app/Activity;)V');
  entry(base, 'public final J(Ljava/lang/String;)V', '    invoke-static {p0, p1}, Lprivacy/SavedGyms;->route(Landroid/app/Activity;Ljava/lang/String;)V');
  entry('smali_classes2/nf/d.smali', 'public j()V', '    invoke-static {p0}, Lprivacy/CheckInTrace;->request(Ljava/lang/Object;)V');
  entry('smali_classes2/nf/f.smali', 'public final m()V', '    invoke-static {p0}, Lprivacy/CheckInTrace;->refresh(Ljava/lang/Object;)V');
  entry('smali_classes2/nf/f.smali', 'public i(Ljava/io/IOException;Lqo/y1;)V', '    invoke-static {p0, p1, p2}, Lprivacy/CheckInTrace;->error(Ljava/lang/Object;Ljava/lang/Throwable;Ljava/lang/Object;)V');
  edit('smali_classes2/ke/e.smali', text => {
    const needle = '    iget v3, p2, Lqo/y1;->a:I';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, needle + '\n    invoke-static {v0, v3}, Lprivacy/CheckInTrace;->response(Ljava/lang/Object;I)V');
  });
  entry('smali_classes2/bq/a.smali', 'public e(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Lorg/json/JSONObject;Lcom/innovatise/utils/j0;)V', '    invoke-static {p1}, Lprivacy/CheckInTrace;->eligibility(Landroid/content/Context;)V');
  entry('smali_classes2/com/innovatise/utils/g0.smali', 'public l(Luf/k;Ljava/lang/Object;)V', '    invoke-static {}, Lprivacy/CheckInTrace;->eligibilityResponse()V');
  entry('smali_classes2/com/innovatise/utils/g0.smali', 'public t(Luf/k;Lnf/g;)V', '    invoke-static {}, Lprivacy/CheckInTrace;->eligibilityError()V');
  // Route eligibility location challenges through the saved-venue helper.
  edit('smali_classes2/bq/a.smali', text => {
    const needle = '    invoke-virtual {p1, p2, v0}, Landroid/support/v4/media/session/r;->e(Landroid/content/Context;Lcom/google/android/material/datepicker/i;)V';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, '    invoke-static {p1, p2, v0}, Lprivacy/AndroidLocation;->request(Landroid/support/v4/media/session/r;Landroid/content/Context;Lcom/google/android/material/datepicker/i;)V');
  });
  // Block every caller of the original shared Android location entry point.
  method('smali/android/support/v4/media/session/r.smali', 'public e(Landroid/content/Context;Lcom/google/android/material/datepicker/i;)V', '    .locals 0\n    invoke-static {p1}, Lprivacy/AndroidLocation;->disabled(Landroid/content/Context;)V\n    return-void');
  const event = 'smali_classes2/com/innovatise/utils/f0.smali';
  // Shared usage/event-triggered app-rating popup entry point.
  method('smali_classes2/com/innovatise/utils/w.smali', 'public final x(Landroid/content/Context;Ljava/lang/String;)V');
  const scanner = 'smali_classes2/com/innovatise/qrcodescanner/QRCodeScannActivity.smali';
  edit('smali_classes2/com/innovatise/home/NFCReader.smali', text => {
    const re = /^\.method public final onCreate\(Landroid\/os\/Bundle;\)V\n[\s\S]*?^\.end method/gm;
    assert.equal([...text.matchAll(re)].length, 1);
    return text.replace(re, body => {
      assert.equal((body.match(/    return-void/g) || []).length, 1);
      return body.replace('    return-void', installHook + '    return-void');
    });
  });
  edit(scanner, text => {
    const re = /^\.method public final onCreate\(Landroid\/os\/Bundle;\)V\n[\s\S]*?^\.end method/gm;
    assert.equal([...text.matchAll(re)].length, 1);
    return text.replace(re, body => {
      assert.equal((body.match(/    return-void/g) || []).length, 1);
      return body.replace('    return-void', '    invoke-static {p0}, Lprivacy/SavedGyms;->install(Landroid/app/Activity;)V\n    return-void');
    });
  });
  edit('smali_classes2/vp/l.smali', text => {
    const needle = '    invoke-virtual {v0, p1}, Lcom/innovatise/qrcodescanner/QRCodeScannActivity;->T(Ljava/lang/String;)V';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, '    invoke-static {p1}, Lprivacy/SavedGyms;->scanned(Ljava/lang/String;)V\n' + needle);
  });
  edit('smali_classes2/com/innovatise/accessControl/AccessControlActivity.smali', text => {
    const needle = '    iput-object p1, v2, Lgf/a;->r:Lorg/json/JSONObject;\n';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, needle + bindHook);
  });
  edit('smali_classes2/com/innovatise/accessControl/b.smali', text => {
    const needle = '    invoke-virtual {v1}, Ljg/d;->z()Lcom/innovatise/utils/f0;';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, successHook + needle);
  });
  edit(scanner, text => {
    const re = /invoke-virtual (\{(?:v0|p1)\}), Lul\/f;->c\(\)V/g;
    assert.equal([...text.matchAll(re)].length, 2, 'Scanner resume call count');
    return text.replace(re, 'invoke-static $1, Lprivacy/CameraPolicy;->resume(Lul/f;)V');
  });
  method(scanner, 'public final onRequestPermissionsResult(I[Ljava/lang/String;[I)V', `    .locals 1
    invoke-super {p0, p1, p2, p3}, Lcom/innovatise/utils/w;->onRequestPermissionsResult(I[Ljava/lang/String;[I)V
    const/16 v0, 0x64
    if-ne p1, v0, :done
    array-length v0, p3
    if-eqz v0, :done
    const/4 v0, 0x0
    aget v0, p3, v0
    if-nez v0, :denied
    iget-object v0, p0, Lcom/innovatise/qrcodescanner/QRCodeScannActivity;->V:Lcom/journeyapps/barcodescanner/DecoratedBarcodeView;
    if-eqz v0, :done
    iget-object v0, v0, Lcom/journeyapps/barcodescanner/DecoratedBarcodeView;->a:Lcom/journeyapps/barcodescanner/BarcodeView;
    invoke-static {v0}, Lprivacy/CameraPolicy;->resume(Lul/f;)V
    goto :done
    :denied
    invoke-virtual {p0}, Lcom/innovatise/qrcodescanner/QRCodeScannActivity;->Z()V
    :done
    return-void`);
  for (const sig of ['public static c(Ljava/lang/String;Ljava/lang/String;)V',
    'public final a(Ljava/lang/Object;Ljava/lang/String;)V', 'public final b(Ljava/lang/String;Ljava/lang/Object;)V',
    'public final d(Ljava/lang/Object;Ljava/lang/String;)V', 'public final f()V', 'public final g()V',
    'public final h(Lnf/g;)V', 'public final i(Luf/k;Z)V', 'public final j(Lcom/innovatise/utils/SourceInfo;)V', 'public final k()V']) method(event, sig);
  method(event, 'public final e()Lcom/amazonaws/mobileconnectors/kinesis/kinesisrecorder/KinesisRecorder;', '    .locals 1\n    const/4 v0, 0x0\n    return-object v0');
  method(event, 'public constructor <init>()V', `    .locals 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    new-instance v0, Ljava/util/HashMap;
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
    iput-object v0, p0, Lcom/innovatise/utils/f0;->a:Ljava/util/HashMap;
    new-instance v0, Ljava/util/HashMap;
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
    iput-object v0, p0, Lcom/innovatise/utils/f0;->b:Ljava/util/HashMap;
    new-instance v0, Ljava/util/HashMap;
    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
    iput-object v0, p0, Lcom/innovatise/utils/f0;->c:Ljava/util/HashMap;
    sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
    iput-object v0, p0, Lcom/innovatise/utils/f0;->e:Ljava/lang/Boolean;
    iput-object v0, p0, Lcom/innovatise/utils/f0;->f:Ljava/lang/Boolean;
    return-void`);
  method('smali/com/amazonaws/mobileconnectors/kinesis/kinesisrecorder/KinesisRecorder.smali', 'public final b(Ljava/lang/String;[B)V');
  method('smali/com/amazonaws/mobileconnectors/kinesis/kinesisrecorder/AbstractKinesisRecorder.smali', 'public final declared-synchronized c()V');
  method('smali/com/pairip/licensecheck/LicenseClient.smali', 'public static checkLicense(Landroid/content/Context;)V');
  edit('smali_classes2/com/innovatise/home/MainActivity.smali', text => {
    const needle = '    :cond_c\n    sget-object p1, Lcom/google/firebase/messaging/FirebaseMessaging;->l:Lmd/i;';
    assert.equal(text.split(needle).length, 2);
    return text.replace(needle, '    :cond_c\n    goto :goto_2\n\n    sget-object p1, Lcom/google/firebase/messaging/FirebaseMessaging;->l:Lmd/i;');
  });
  method('smali_classes2/pi/a.smali', 'public constructor <init>(Landroid/content/ContextWrapper;)V', `    .locals 1
    const/4 v0, 0x0
    iput v0, p0, Lpi/a;->a:I
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    invoke-static {p1}, Lprivacy/Identity;->get(Landroid/content/Context;)Ljava/util/UUID;
    move-result-object v0
    sput-object v0, Lpi/a;->d:Ljava/util/UUID;
    return-void`);
  edit('smali_classes2/com/innovatise/utils/b.smali', text => {
    const key = 'SHARED_PREFERENCE_DEVICE_UUID_FOR_ALL_REQUEST';
    assert.equal(text.split('"' + key + '"').length, 2);
    return text.replace('"' + key + '"', '"' + key + '_PRIVACY_V1"');
  });
  // Redirect setters without modifying live registers in the original methods.
  const redirects = [
    ['invoke-virtual', 'Landroid/webkit/WebSettings;->setAllowFileAccess(Z)V', 'Lprivacy/WebPolicy;->fileAccess(Landroid/webkit/WebSettings;Z)V', 2],
    ['invoke-virtual', 'Landroid/webkit/WebSettings;->setAllowContentAccess(Z)V', 'Lprivacy/WebPolicy;->contentAccess(Landroid/webkit/WebSettings;Z)V', 1],
    ['invoke-virtual', 'Landroid/webkit/WebSettings;->setMixedContentMode(I)V', 'Lprivacy/WebPolicy;->mixedContent(Landroid/webkit/WebSettings;I)V', 1],
    ['invoke-virtual', 'Landroid/webkit/CookieManager;->setAcceptThirdPartyCookies(Landroid/webkit/WebView;Z)V', 'Lprivacy/WebPolicy;->thirdPartyCookies(Landroid/webkit/CookieManager;Landroid/webkit/WebView;Z)V', 1],
    ['invoke-static', 'Landroid/webkit/WebView;->setWebContentsDebuggingEnabled(Z)V', 'Lprivacy/WebPolicy;->debug(Z)V', 1]
  ];
  const all = walk(path.join(decoded, 'smali_classes2/com/innovatise')).filter(f => f.endsWith('.smali'));
  for (const [opcode, from, to, expected] of redirects) {
    let total = 0;
    for (const file of all) {
      const text = fs.readFileSync(file, 'utf8');
      const re = new RegExp(escape(opcode) + ' (\\{[^}]+\\}), ' + escape(from), 'g');
      const count = [...text.matchAll(re)].length;
      if (count) { total += count; edit(path.relative(decoded, file), s => s.replace(re, 'invoke-static $1, ' + to)); }
    }
    assert.equal(total, expected, `Web-policy call count changed: ${from}`);
  }
  for (const name of ['Identity', 'WebPolicy', 'CameraPolicy', 'AndroidLocation']) {
    const dest = path.join(decoded, 'smali_classes2/privacy', name + '.smali');
    fs.mkdirSync(path.dirname(dest), {recursive: true});
    fs.copyFileSync(path.join(root, 'patches', name + '.smali'), dest);
    log.push({file: path.relative(decoded, dest), added: true, after: hash(fs.readFileSync(dest))});
  }
  edit('AndroidManifest.xml', xml => {
    assert.equal((xml.match(/<uses-permission\b[^>]*android:name="android.permission.CAMERA"[^>]*\/>/g) || []).length, 1, 'Scanner requires CAMERA declaration');
    const labels = 'android:label="@string/app_name"';
    assert.equal(xml.split(labels).length - 1, 17, 'Unexpected application/activity label count');
    xml = xml.replaceAll(labels, 'android:label="Derriumuts Unleashed"');
    for (const name of removedPermissions) {
      const re = new RegExp('\\s*<uses-permission\\b[^>]*android:name="' + escape(name) + '"[^>]*/>', 'g');
      assert.equal([...xml.matchAll(re)].length, 1, `Permission mismatch: ${name}`); xml = xml.replace(re, '');
    }
    for (const [tag, name] of [
      ['service', 'com.innovatise.fcm.MFFcmMessagingService'],
      ['service', 'com.google.firebase.messaging.FirebaseMessagingService'],
      ['receiver', 'com.google.firebase.iid.FirebaseInstanceIdReceiver']]) {
      const re = new RegExp('<' + tag + '\\b[^>]*android:name="' + escape(name) + '"[^>]*(?:/>|>[\\s\\S]*?</' + tag + '>)', 'g');
      assert.equal([...xml.matchAll(re)].length, 1); xml = xml.replace(re, '');
    }
    // Development preview is not an externally callable user feature.
    const preview = /<activity\b[^>]*android:name="androidx\.compose\.ui\.tooling\.PreviewActivity"[^>]*\/>/g;
    assert.equal([...xml.matchAll(preview)].length, 1);
    xml = xml.replace(preview, tag => tag.replace('android:exported="true"', 'android:exported="false"'));
    const metadata = {firebase_messaging_auto_init_enabled: 'false', firebase_data_collection_default_enabled: 'false',
      firebase_analytics_collection_enabled: 'false', firebase_analytics_collection_deactivated: 'true', google_analytics_adid_collection_enabled: 'false'};
    for (const [name, value] of Object.entries(metadata)) {
      const re = new RegExp('<meta-data\\b[^>]*android:name="' + name + '"[^>]*/>', 'g');
      const tag = `<meta-data android:name="${name}" android:value="${value}"/>`;
      if (re.test(xml)) xml = xml.replace(re, tag); else xml = xml.replace('</application>', tag + '\n</application>');
    }
    return xml;
  });
  return log;
}
export function walk(dir) {
  return fs.readdirSync(dir, {withFileTypes: true}).sort((a,b) => a.name < b.name ? -1 : 1).flatMap(e => e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
}
