# Privacy and compatibility

The scissors have a job description. Here is what they trim—and what remains. (・ω・)

Applies to Derrimut 110.60 / version code 705800 with the inputs pinned in
`build-lock.json`.

## Data handling

| Area | Treatment |
|---|---|
| Event reporting | Identified event collection, queueing and Kinesis upload paths disabled |
| Device identity | Random persisted installation ID replaces the identified hardware-derived ID path |
| Push registration | Identified Firebase token request and push components disabled |
| Permissions | Location, phone state, calendar, legacy account, external storage and other unnecessary declarations removed |
| Embedded web content | Identified settings reject third-party cookies, local file/content access and mixed content; payment WebView debugging disabled |
| Rating prompts | Automatic rating prompt disabled |
| Saved gyms | Entries stored in app preferences; saving offered after the original success callback |
| Location | Selected venue coordinates passed through the original eligibility callback; Android location provider access disabled |
| Diagnostics | Bounded local stage/error records; no request bodies, coordinates, tokens or membership details recorded |

Saved venue selection is scoped to the originating activity and matching route,
expires after two minutes, and is consumed once. A physical QR scan clears
pending selections. Missing venue metadata does not trigger Android location
access. Venue coordinates are not a measurement of the device's position.

## What still leaves the phone

Account and access services still receive authentication and check-in data,
including selected venue coordinates when required. They may retain IP
addresses, timestamps, installation identifiers and visits. A random installation
ID does not make an authenticated account anonymous.

Firebase, AWS, MQTT and native libraries remain where needed for compatibility.
Disabling identified collection paths does not prove that all third-party
traffic is absent. Embedded pages and external browsers may collect their own
data. Existing server records are not removed.

## Things that may need patience

Push notifications, device-location discovery, calendar integration and some
storage features may be unavailable. Browser restrictions may affect SSO,
payments, uploads and mixed-content media. A changed installation ID or signing
key may require login or device registration.

Membership checks, authorization decisions and success/failure handling remain
in the original application/server flow. Saved-code and venue data may change
and require maintenance. Local assembly checks do not establish real-device or
turnstile behavior.

## Build checks

Input and tool hashes are pinned. Builds preserve original resource and native
library bytes, verify ZIP integrity/alignment, compare protected classes after
removing allowed observer hooks, and resolve Java helper signatures referenced
by smali. Tests cover URL handling, request correlation and saved-location
selection. Reproducibility compares two clean unsigned APKs.

See `VALIDATION.json` for the delivered artifact's checks and runtime-test status.
