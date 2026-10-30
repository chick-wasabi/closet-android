# Testing

`python3 -m unittest discover -s scripts/tests` checks missing-identity rejection, regeneration protection, and secret-input redaction.

`./gradlew testDebugUnitTest` and `./gradlew --max-workers=1 lintDebug` run local matching tests, native Compose render checks using Robolectric/Roborazzi, and Android static analysis. Renders are written to `app/build/outputs/design-review/`.

Lint runs serially to avoid concurrent Kotlin frontend analysis failures. No lint source sets or checks are disabled.

The render matrix covers compact 320 dp screens, 360 dp screens, wider 600 dp layouts, landscape, dark mode, and font scaling up to 2×. Header checks inspect actual text layouts for wrapping and overflow. These checks do not replace physical-device testing or guarantee every manufacturer's behavior.

Refresh public screenshots with `python3 scripts/update_screenshots.py`. Its `--check` option checks a UI-source fingerprint and required files; read-only CI rejects stale screenshot documentation. It does not grant a workflow permission to push to the repository.

`./gradlew connectedDebugAndroidTest` checks navigation, editing, database persistence, and outfit PNG composition on Android. Camera apps, model provisioning, cutout quality, and share targets also need device testing.

Packaging debug and release APKs requires the existing permanent signing configuration. `scripts/signing.py verify-apks` verifies both against the certificate pin. Do not remove signing checks to make a build pass.

GitHub Actions uploads APKs separately from validation reports and screenshots. It does not publish a Play Store release.
