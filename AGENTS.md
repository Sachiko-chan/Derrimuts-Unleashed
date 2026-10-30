# Maintenance

- Prefix new PR branches with `feature-`.
- Keep filenames concise; put related files in a directory instead of repeating
  the directory name in every filename.
- Never commit input APKs, decrypted/decompiled upstream app files, private
  signing keys, passwords, logs, `.cache`, `.work`, or generated output.
- Do not relax input/tool hash checks to make a different app version build.
  Review and update the lock and patch expectations explicitly for new versions.
- Preserve original merged resources/native libraries byte-for-byte. Do not
  ship Apktool-rebuilt resources from a base-only decode.
- Preserve gate authorization and membership checks. Device identity and
  web-view changes can affect authentication; document that risk honestly.
- Run `node --test tests/*.test.mjs` and a two-clean-build comparison for changes
  affecting APK bytes. Verify signatures separately after signing.
- Never claim runtime, login or gate testing from assembly/signature checks.
- Avoid new dependencies unless they are necessary and hash-pinned.

- Permit exactly two access-control observer hooks; removing those exact
  additions must reproduce original protected-class hashes. Never change
  authorization, membership validation or success/failure branches.

- Permit exactly one SavedGyms.install call in NFCReader.onCreate;
  all other original NFC instructions must remain unchanged.
