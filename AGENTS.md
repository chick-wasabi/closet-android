# Repository instructions

Read CONTRIBUTING.md and docs/SIGNING.md before changes.

Preserve applicationId dev.closet.personal and the pinned signing certificate. Never distribute a generated runner key, replace a missing key, commit private signing material, or rely only on a cache. Missing/mismatched credentials must fail packaging. Debug and release use the same identity. Increase versionCode for native updates.

For every new Android project, establish and securely back up its own permanent signing identity before distributing its first APK. Apply skills/android-release-identity/SKILL.md; do not copy Closet's private key into unrelated apps.

Keep app copy concise, surfaces rounded, and motion finite. Measure headings with the actual font and system font scale. Check 320–600 dp, landscape, dark mode, and 2× text. Inspect actual renders. Refresh docs/screenshots with scripts/update_screenshots.py; public images must match the UI. Public docs describe the app, not private discussion history or one owner's phone.

Run ./gradlew testDebugUnitTest and ./gradlew --max-workers=1 lintDebug for relevant changes. Packaging also requires the permanent signing configuration. Preserve unrelated work. Use existing checkouts, not new worktrees unless requested. Commit cohesive verified changes and push main; never force push. Pushes trigger the configured APK workflow, not a Play Store release.
