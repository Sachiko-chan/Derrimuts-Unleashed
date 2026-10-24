# Derriumuts Unleashed

**Less tracking. More lifting. (ง •̀_•́)ง**

こんにちは、Sachikoです。I wanted to go to the gym. My phone apparently had a
whole extracurricular programme planned. So, I brought scissors. Carefully. ✂️

This is a privacy patch project for Derrimut **110.60 / 705800**. It builds one
ARM64 APK, keeps the gym's membership checks, and trims the data collection and
permissions identified in the supplied app. The displayed name is
**Derriumuts Unleashed**; the package stays `com.innovatise.derrimut` so our
modified builds can update one another when signed with the same key.

Unofficial, and not affiliated with the gym or the original app developer.

**Current status:** r7 launches and shows Saved gyms, but both QR and saved-code
check-in were reported to hang. **r8 is a diagnostic build, not a confirmed fix
for that hang.** Gate operation has not been verified here.

## Location disabled — r10

The dedicated `privacy.AndroidLocation.request` helper accepts optional,
nullable `gym` metadata. It does not request Android location or supply a
replacement location. The original shared location entry point is also blocked.
Location permissions remain removed. If a server requests location, the spinner
is dismissed and a clear message explains that the operation cannot continue.
This may prevent location-dependent gym check-in; it does not bypass that rule.
Seed data is unchanged. No nationwide coordinate list has been added.

## Check-in diagnostics — r8

The saved-code route on the NFC screen now sets the same QR scan-type marker
as the original QR scanner. This fixes a missing step in our added route;
it does not explain the physical scanner hanging too.

Both entry screens have a **Check-in diagnostics** button. A pending attempt
also shows a diagnostic dialog after 45 seconds, with **Copy details** so a
phone alone is enough. It records request stages, HTTP status codes and broad
network error categories locally. No URLs, credentials, response bodies or
membership details are captured. The last report survives an app restart.
It is overwritten on the next attempt and is never uploaded automatically.

The notice does not declare success, cancel a request, or automatically retry.
Use **Keep waiting** to dismiss it; copying details does not stop the request.
A compiler still cannot walk through a turnstile for us. (・ω・)

## Rating and check-in fixes — r7

The automatic “rate this app” prompt is disabled, including event-triggered
reminders. One less interruption. ( ˘ω˘ )

**Saved gyms** now appears directly below the toolbar on both NFC and QR
check-in screens, including when “Always Tap Phone” opens NFC automatically.

## Saved gyms — r6

Open check-in, tap **Saved gyms**, choose **Windsor Gardens**, then tap
**Check in**. No need to aim the camera at the same square again. (｡•̀ᴗ-)✧
QR scanning and NFC remain available. An internet connection and an authorized
membership are still required.

After an unknown HFF QR code is scanned and its access request succeeds, the
app offers **Save this gym**. Give it a name; next time it appears in the dropdown.
The list is stored in app preferences, with **Manage → Rename/Delete** controls.
Duplicate links are combined; clearing app data removes personal entries.
The bundled Windsor Gardens link was supplied by the user, not gate-tested here.
Only HTTPS `hff.deeep.link/hff-…` links without query strings are saveable in
this version. A saved link can stop working if the provider changes it.

## Camera check-in fix — r5

The scissors went a little too far. Camera permission is back for QR check-in.
Open the scanner and allow camera access when Android asks. (｡•̀ᴗ-)✧
If access was previously blocked, enable Camera in Android Settings → Apps →
Derriumuts Unleashed → Permissions, then reopen check-in.

Camera startup now waits for permission; granting it resumes the scanner.
An interrupted permission request no longer indexes an empty result array.
Install over the previous modified APK to preserve login and the random ID.
This revision still needs testing on your phone and at the gate.

## What got a little tidier

- **Event reporting:** disables collection, queue creation and Kinesis
  writes/uploads through the identified paths, including access and NFC-scan
  telemetry.
- **Push registration:** skips the identified Firebase token request and removes
  the push components.
- **Permissions:** removes 15 declarations, including location, phone state,
  calendar, external storage, legacy account access and boot reception.
- **Device identity:** replaces the Android-ID/phone-ID-derived UUID with a
  random, persisted installation UUID. A new request-cache key prevents that
  path from reusing the older device-derived value after an upgrade.
- **Embedded web pages:** redirects the identified settings to reject third-party
  cookies, ordinary local-file access, content-provider access, and mixed
  HTTP/HTTPS content. It also disables the payment screen's WebView debugging.
- **Loose ends:** makes the development preview activity non-exported and disables
  the local Play licence-check entry point for the re-signed build.
- **A new name:** updates application/activity labels while retaining the package
  ID and original version.

NFC and membership checks are preserved. Two access-control observer hooks
associate scanned links with requests and offer saving after confirmed success.
**You still need a valid membership and server authorization.** The random UUID
is still an identifier, and signing in still connects activity to your account.

[AUDIT.md](AUDIT.md) covers the evidence, remaining data exposure and possible
regressions. Less tracking is the goal; “magically anonymous” is not a promise
I'm making.

## Build it yourself — 準備しましょう

On **Linux x86-64**, you need **Node.js 20+**, **Java 17**, `curl`, and `dpkg-deb`
(the last one is only for signing). No Android Studio, Gradle, npm packages,
Python orchestration, or root required. A surprisingly small toolbox.

1. Put your original `com.innovatise.derrimut.zip` in `input/`. It must contain
   these four files at the ZIP's root:

   ```text
   base.apk
   config.arm64_v8a.apk
   config.en.apk
   config.xxhdpi.apk
   ```

2. From the repository directory, run:

   ```sh
   node --test tests/*.test.mjs
   node build.mjs input/com.innovatise.derrimut.zip --repro-check
   ```

This project supports the exact APKs recorded in `build-lock.json`. Every input
SHA-256 must match. If another version wanders in, the build stops rather than
trying its luck with someone else's bytecode. Sensible little thing.

The first run downloads hash-pinned **Apktool 2.11.1**, **APKEditor 1.4.9**, the ECJ compiler, D8 and
Android/JSON compilation API jars into
`.cache/`. Later builds can use that verified cache offline.

`--repro-check` performs **two independent decodes, patches, builds and merges**,
compiles the saved-gym extension from Java, then compares the complete unsigned APK bytes. Any difference fails the build.

Your results appear here:

| File | What's inside |
|---|---|
| `output/Derriumuts-Unleashed-110.60-arm64-r10-unsigned.apk` | The unsigned app |
| `output/SHA256SUMS` | Its checksum |
| `output/build-report.json` | Toolchain, input/tool hashes and checks |
| `output/patch-log.json` | Changed-file hashes |

**The unsigned APK needs signing before installation.** The repository contains
no original APK, credentials, signing key, decompiled app tree, or downloaded
third-party build tools. Bring your own original app; the patches live here.

## Give it your signature

Android recognizes updates by their signing key. **Use the same key as the
installed modified build** if you want to update it without uninstalling.

For the maintainer continuing the earlier builds, that is the
`derrimut-private.p12` key in the original private backup ZIP. Keep the key,
password and backup ZIP outside this repository. They are not party favours.

In Bash:

```sh
export KEYSTORE=/absolute/private/path/derrimut-private.p12
export KEY_ALIAS=privacy
read -rsp 'Signing password: ' KEYSTORE_PASSWORD; echo
export KEYSTORE_PASSWORD
node sign.mjs
unset KEYSTORE_PASSWORD
```

Starting an independent distribution? Create your own key outside the repo,
then point `KEYSTORE` at it:

```sh
keytool -genkeypair -storetype PKCS12 -keystore /private/path/unleashed.p12 \
  -alias privacy -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=Derriumuts Unleashed'
```

`sign.mjs` downloads checksum-pinned Android signing tools, signs with v2/v3,
and verifies the result. Optional input/output paths are accepted. Passwords
are passed through environment variables, not command-line arguments or source
files. Set `KEY_PASSWORD` only if the key uses a separate password.

The signed output is `output/Derriumuts-Unleashed-110.60-arm64-r10.apk`, alongside
its checksum.

A different signing key cannot update an existing installation. The official
app also uses a different key; uninstalling it clears its local data and login.
Everyone installing this build needs their own valid gym membership and login.

## Two matching builds, please (｡•̀ᴗ-)✧

The reproducibility target is the **unsigned APK**, using identical original
inputs, patch source, pinned tools, and a compatible host toolchain. Two clean
builds were compared during delivery; `VALIDATION.json` records the exact
Node/Java versions and resulting digest. Run `--repro-check` on your machine too.

Cross-OS/toolchain byte identity has not been established. Signed-byte identity
also depends on the key, signer version and signature algorithm. Generating a
new key will produce a different signed APK, even with identical app contents.

There is one packaging detail I am particularly fussy about: **the original
resources stay intact**. An earlier base-only decode lost references to images
in the density split, which made the club-search screen crash. That fix belongs
in the build, not on a sticky note next to somebody's keyboard.

Apktool edits the code and manifest, but **none of its rebuilt resources are
shipped**. The final APK retains the original merged resource table, compiled
XML, images and native libraries byte-for-byte. Only the manifest and DEX files
come from the patched build. ZIP timestamps, order, permissions and padding
are normalized; stored entries use four-byte alignment and native libraries
use 16 KiB alignment.

## A home on GitHub

Extract the repository ZIP into an empty repo, including `.github/` and
`.gitignore`, then commit the source files. Keep `.cache/`, `.work/`, local files
in `input/`, `output/`, backup ZIPs and private keys out of your commits. The
empty `input/.gitkeep` is meant to stay.

The included Actions workflow runs **manually**. Set the repository Actions
secret `ORIGINAL_APK_URL` to a private HTTPS download URL for the original ZIP.
The workflow downloads it temporarily, checks all four APK hashes, performs two
builds and uploads **unsigned artifacts**.

It does not sign or publish a release, and it does not run on untrusted pull
requests. Treat the artifacts as distributable app binaries; the original app
remains subject to its owner's terms. Building entirely locally is also fine.
GitHub does not need to be invited to every workout.

## Before we celebrate

Install the signed APK over r3 using the same key. Check launch, club selection,
login, account loading, and NFC entry while gym staff are available.

Also test the embedded SSO/payment pages you use. Third-party cookie restrictions
can affect them; file/content restrictions can affect attachments. The new
installation UUID may make the backend recognize a new device or ask you to
authenticate again. It stays stable across restarts and ordinary app updates.

If something regresses, install the previous r3 APK with the same key. Its
version code is unchanged, so this preserves app data. It does **not** undo
server-side device registration or revert preferences already written. Clearing
app data resets your login and installation identity; it is not a necessary
first step for rollback.

No build check proves that gym entry works or that every telemetry path is gone.
This is a targeted patch project, not the original app's source code.

少しずつ、ちゃんと。One careful improvement at a time. 🌸
