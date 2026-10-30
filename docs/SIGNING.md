# Android signing and safe updates

All distributed Closet debug and release APKs use one permanent private key and application ID `dev.closet.personal`. Its public certificate digest is pinned in `config/signing-certificate.sha256`.

Android updates require the same application ID, a compatible signing identity, and a versionCode not lower than the installed version. Increment versionCode for native updates. A release build can replace a debug build when these conditions hold; an unrelated key cannot.

## Maintainer setup

Keep the existing signing bundle in a password manager or another secure external backup. Losing it may prevent future updates to installed copies. Never commit it, use an expiring cache as its only copy, or regenerate a key when a secret is missing.

1. In repository **Settings → Secrets and variables → Actions**, add `CLOSET_SIGNING_BUNDLE` with the complete existing signing-bundle JSON as the secret value.
2. The workflow restores it to private temporary storage and verifies its certificate against the pinned digest.
3. Before artifact upload, the APK's actual certificate is verified. Missing or mismatched material stops packaging.

Restore the same bundle for local packaging:

```sh
python3 scripts/signing.py restore \
  --bundle-file /secure/path/closet-signing-bundle.json \
  --output-dir /secure/path/closet-prepared
export CLOSET_SIGNING_CONFIG=/secure/path/closet-prepared/signing.json
./gradlew assembleDebug assembleRelease
python3 scripts/signing.py verify-apks \
  app/build/outputs/apk/debug/app-debug.apk \
  app/build/outputs/apk/release/app-release.apk
```

Passwords and private-key bytes are not printed, committed, or uploaded as artifacts. `scripts/signing.py init` establishes an identity before an application's first distribution; it refuses to replace Closet's existing pin.

## Early temporary-key builds

An APK contains a public certificate, not its private key. A discarded CI debug key cannot be reconstructed from an installed app. Moving such an installation to the permanent identity can require one final reinstall. Subsequent releases must preserve the permanent identity.

If publishing through Google Play App Signing later, preserve the app-signing certificate and distinguish it from the upload key. An unrelated Play signing certificate will not automatically update a sideloaded installation.
