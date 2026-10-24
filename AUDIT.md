# Privacy audit and limits

Reviewed artifact: original Derrimut 110.60 / version code 705800, four APK hashes
in `build-lock.json`. This is a static inspection of Java/Dalvik code and the
manifest. No real account requests, live traffic capture, server probing or
physical-gate testing were performed.

| Finding | Evidence in original code | r5 treatment |
|---|---|---|
| Identity-linked event reporting | `com.innovatise.utils.f0` builds event records with account/user/session identifiers; access callbacks add request bodies and NFC scan URLs | All collection setters, serialization, queueing and upload entry points disabled; constructor no longer creates the event directory |
| AWS recorder transport | `KinesisRecorder.b(String, byte[])` and `AbstractKinesisRecorder.c()` | Write/upload methods return immediately |
| Device-derived identity | `pi.a(ContextWrapper)` derives a UUID from Android ID, falling back to telephony device ID | Replaced with random persisted UUID; no hardware/system-ID read in this constructor |
| Previously cached device value | `utils.b` reads `SHARED_PREFERENCE_DEVICE_UUID_FOR_ALL_REQUEST` | Uses a new privacy-versioned cache key on upgrade |
| Push registration | `MainActivity` explicitly requests a Firebase messaging token | Skipped; messaging services and receiver removed; Firebase collection flags disabled |
| Web tracking/capabilities | Main embedded web view explicitly enables third-party cookies and file/content access | Setter calls redirect to small audited policy methods enforcing false |
| Web transport policy | Main embedded web view sets mixed-content mode | Policy wrapper forces `MIXED_CONTENT_NEVER_ALLOW` (1) |
| Web debugging | `IapWebviewController.onCreate` enables WebView debugging | Wrapper always disables it |
| Exported development screen | Compose `PreviewActivity` exported | Set non-exported |
| Resource regression in r1/r2 | Decoding base alone lost references to drawables defined by density splits | Every original merged resource and native library is retained byte-for-byte |

## What remains

- Membership, account and gate APIs still receive necessary account/access data.
  The backend can log visits, IP addresses, timestamps, installation identifiers
  and requests. The new UUID is pseudonymous, not anonymous: signing in still
  links activity to the membership. App installation IDs and session IDs used
  by business APIs are not globally removed.
- Firebase libraries/initialization remain present for compatibility. Disabled
  flags and identified push paths are not proof of zero Google traffic.
- AWS authentication/configuration, MQTT, media playback and business API code
  remain. Their presence alone was not classified as spyware. Broadly blocking
  all AWS/MQTT traffic without runtime evidence could break required features.
- Embedded web content and external browser pages can have their own tracking.
  This patch is not a URL filter; first-party cookies, JavaScript and required
  authentication bridges remain. Only the identified explicit settings are
  redirected, not every possible dynamic or native web-view path.
- Native libraries were preserved, not exhaustively reverse engineered.
- Existing server records and old local preferences are not erased. A new
  preference namespace stops the reviewed identity paths reusing the older
  UUID. Clearing app data resets the random UUID and also clears login data.
- Remaining manifest permissions: CAMERA, INTERNET, ACCESS_NETWORK_STATE, NFC,
  FOREGROUND_SERVICE, WAKE_LOCK, VIBRATE, Play BILLING/CHECK_LICENSE and the
  app's signature-level dynamic-receiver permission. Declaring a permission
  does not prove it is used. Membership and payment entitlements are unchanged.

## Expected tradeoffs

Push notifications, location discovery, calendar integration
and storage-related features may not work. Stricter embedded-browser policies
can affect third-party SSO, embedded payments, mixed-content media and file
uploads. The UUID change may require re-authentication or device registration;
that cannot be resolved by bypassing gym authorization. Test these explicitly.

The only reported phone test so far is successful launch of r3. No claim is made
that r5 has been runtime-tested, that NFC entry works, or that the app is
spyware-free. Follow-up work should compare traffic and logcat during launch,
login, an idle interval, backgrounding and NFC use on a real device.

## Implementation references

- [Android WebSettings](https://developer.android.com/reference/android/webkit/WebSettings)
- [Android CookieManager](https://developer.android.com/reference/android/webkit/CookieManager)
- [Android WebView debugging](https://developer.android.com/reference/android/webkit/WebView#setWebContentsDebuggingEnabled(boolean))
- [Apktool](https://apktool.org/)
- [APKEditor split merging](https://github.com/REAndroid/APKEditor)

The security rationale follows these API behaviors; app-specific findings above
come from the supplied APK, not from assumptions based on permissions alone.

## r5 camera regression and lifecycle review

Restores the original CAMERA manifest permission, required by the existing
request code in QRCodeScannActivity and CustomerCleverWebViewActivity.
The native scanner retains its original contextual request and denial UI.
Both scanner resume call sites now check permission before opening the camera.
The permission callback explicitly resumes on grant and safely ignores empty
results instead of indexing element zero. No microphone permission is added.
Other privacy restrictions remain; no additional broad removals were made.
Phone permission/denial/settings-return and actual QR entry require runtime testing.

## r6 saved gyms

Windsor Gardens is seeded with the user-supplied HTTPS HFF link. Additional
entries are local app preferences, not uploaded by this extension.
Existing provider APIs still receive check-in requests.
The scanner callback records a recent physical scan; request construction
consumes that candidate only for its matching URL. A weak request-identity map
connects it to the existing Success=true UI callback. Failures, transport-only
success, unrelated requests and duplicate callbacks do not offer saving.
Candidates expire after five minutes and are not persisted.
Only HTTPS hff.deeep.link/hff-… links without credentials, ports, query strings
or fragments are eligible. URL tokens remain case-sensitive.
The two access-control additions only observe request identity and success;
removing their exact instruction sequences must reproduce the original class
hashes. API paths, authorization and membership branches remain unchanged.

The helper Java classes compile into classes3.dex using hash-pinned ECJ/D8.
Android and JSON API jars are compile-only; no replacement platform libraries
are bundled. Tests exercise validation, deduplication keys, expiry and one-shot
scan correlation. Real-device UI, QR/NFC and gym entry still need testing.

## r7 rating prompt

The shared rating-prompt method `utils.w.x(Context, String)` now returns
immediately. This disables both usage-based and event-triggered prompts,
including their rating counters. Other dialog behavior is preserved. Confirmed by inspecting the final DEX; no phone test was performed.

The saved-gym control is installed directly in the shared native check-in
layout below the toolbar, on both QRCodeScannActivity and NFCReader.
NFCReader receives one UI-install call; its original instructions otherwise
remain identical. Saved links use its existing J(String) URL resolver.
The build removes this exact hook before comparing protected-class hashes.

## r8 investigation and diagnostics

Compared the supplied original base APK with the patch set. QR and saved NFC
entry share utils/w.J, anonymous proxy auth, deep-link metadata resolution and
eligibility routing. No confirmed cause of the reported shared hang was found.
The NFC saved-code helper omitted SHARED_PREFERENCE_SCAN_TYPE=QR; corrected
to match QRCodeScannActivity.T. This alone cannot explain physical QR hanging.

Added passive, local, bounded (32 events) CheckInTrace hooks at resolver entry,
proxy requests/responses/errors, auth refresh, resolved metadata, eligibility
callbacks, module routing and original spinner dismissal. No request data or
member identifiers enter the logger. A 45-second notice offers copying details;
no synthetic success, authorization bypass, automatic retry, or cancellation.
Original resources/native libraries and protected gate classes retain their
existing preservation checks. Runtime and gate validation remain outstanding.

## r10 location-disabled helper

Based on delivered r8; both location permissions remain removed.
privacy/AndroidLocation.request exposes default and nullable gym-metadata
overloads. Neither calls Android location nor manufactures callback results.
All callers of the original session/r.e shared provider entry point now reach
the same disabled handler, which dismisses progress and explains that the
operation cannot continue with location disabled. Seed data is unchanged.
No server eligibility or membership outcome is bypassed.
