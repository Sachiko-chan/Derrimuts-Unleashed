# Derriumuts Unleashed

**Less tracking. More lifting. (ง •̀_•́)ง**

A privacy-focused patch for Derrimut 110.60 (version code 705800), producing a
single ARM64 Android APK. Gym time, with fewer digital tag-alongs.

Unofficial and not affiliated with the gym or app developer.

## A little less nosy (・ω・)

- QR, NFC and saved-gym check-in using the existing membership and authorization flow.
- Save a scanned gym after a successful check-in; rename or delete saved entries.
- Saved venue coordinates for saved-gym check-in, with Android location access disabled.
- Reduced permissions, identified telemetry paths disabled, and a random installation ID.
- Stricter embedded-browser settings and no automatic app-rating prompts.
- Local check-in diagnostics with a copy button.

## Check in and get moving

Open **Check-in → Saved gyms**, select a gym, and tap **Check in**.
Use **Manage** to rename or delete an entry. After successfully scanning a new
supported gym code, choose **Save gym** to add it to the list.

A valid membership, login and internet connection are required. Saved entries
stay on the device. Selected venue coordinates are sent when the check-in service
requests location; the app does not read the phone's position. An entry without
matching venue data cannot complete a location-dependent check-in.

QR scanning requires Camera permission. If previously denied, enable it in
Android's app settings and reopen check-in. Use **Check-in diagnostics → Copy
details** when troubleshooting. Diagnostics stay local and are never uploaded
automatically.

## Build — 準備しましょう

A small toolbox, please: Linux x86-64, Node.js 20+, Java 17 and curl. Signing also requires
`dpkg-deb`. Android Studio, Gradle and npm packages are not required.

Place the original split bundle in `input/original.zip`, containing these files
at its root: `base.apk`, `config.arm64_v8a.apk`, `config.en.apk`, and
`config.xxhdpi.apk`. Input hashes must match `build-lock.json`.

```sh
node --test tests/*.test.mjs
node build.mjs input/original.zip --repro-check
```

The build downloads hash-pinned tools into `.cache/`. The reproducibility check
performs two clean builds and requires byte-identical unsigned APKs. Matching twins,
please. (｡•̀ᴗ-)✧

| Output | Contents |
|---|---|
| `output/Derriumuts-Unleashed-110.60-arm64-r13-unsigned.apk` | Unsigned APK |
| `output/SHA256SUMS` | APK checksum |
| `output/build-report.json` | Toolchain and validation results |
| `output/patch-log.json` | Changed-file hashes |

The GitHub Actions workflow is started manually. Set the repository secret
`ORIGINAL_APK_URL` to an HTTPS download URL for the original bundle, select the
branch to build, and run **Reproducible unsigned APK**. Download its output artifact.

## Give it your signature ✍️

Sign with your existing key to update an installed modified build without
clearing its data. Keep keys and passwords outside the repository—those are
not party favours. ( ˘ω˘ )

```sh
export KEYSTORE=/private/path/unleashed.p12
export KEY_ALIAS=privacy
read -rsp 'Signing password: ' KEYSTORE_PASSWORD; echo
export KEYSTORE_PASSWORD
node sign.mjs
unset KEYSTORE_PASSWORD
```

`sign.mjs` signs with APK Signature Scheme v2/v3 and verifies the result. Set
`KEY_PASSWORD` if the key uses a separate password. Optional input and output
paths may be passed as arguments. The official app and differently signed
builds cannot update one another directly.

## The small print, neatly folded

See [AUDIT.md](AUDIT.md) for current data handling and compatibility limits.
[VALIDATION.json](VALIDATION.json) records the delivered build's checks.
Original resources and native libraries are preserved byte-for-byte; protected
gate and membership classes are checked against the original code with only
specified observer hooks allowed.

Build checks do not establish device or turnstile compatibility. Backend
services can still associate check-ins with an account. Dependencies and venue
data require ongoing maintenance.

The repository contains patch/build code only. It excludes original APKs,
decompiled application files, credentials and signing keys. [LICENSE](LICENSE)
covers this project's original code and documentation, not the upstream app.

少しずつ、ちゃんと。One careful improvement at a time. 🌸
