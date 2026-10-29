# Validation record

## Verified in the cloud workspace

- Official Gradle 8.13 wrapper JAR SHA-256 checked against Gradle’s published digest. The distribution checksum is pinned in wrapper properties.
- JDK 21 and Android SDK 36 installed from official sources with artifact verification preserved.
- `./gradlew --no-daemon --max-workers=2 testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest` succeeded.
- **9 JVM tests passed; 0 failed, 0 skipped.** Covers required pieces, strict filters, matching, order, optional policy probabilities, and invalid rule rejection.
- Android lint: **0 errors**. Remaining warnings concern newer dependency versions, kapt-to-KSP migration, and an optional bitmap convenience extension.
- Debug application APK and the Android device-test APK compiled successfully.
- The merged application manifest has **no Internet or network-state permission**. The only Android runtime permission is write-to-shared-storage, limited to Android API 28 and below. App and device-transfer backup exclusions are declared.
- Reusable cloud installation script completed a repeat run with the prepared toolchains and Gradle caches.

## Android device checks

Four instrumentation tests are included: navigation/add entry points; brush + item metadata save; Room persistence across database reopening; transparent stacked PNG dimensions/pixel order.

A Google APIs Android 35 software emulator was started at **1264 × 2780, density 450**, matching the OnePlus 13R screen size/aspect ratio. This host has no `/dev/kvm`, so it cannot accelerate Android emulation. The first device-test attempt encountered an unresponsive property fetch during the emulator’s first boot. A direct ADB install attempt then failed inside Android StorageManagerService because PackageManagerInternal was still uninitialized; Closet never installed or launched. **Zero app device tests executed.** This is an emulator startup limitation, not an observed Closet crash. Visual inspection remains unverified. The emulator was stopped after these attempts.

Run `./gradlew connectedDebugAndroidTest` with a booted emulator or connected Android phone. Tests create and clean up synthetic test records/images; use a test installation rather than a populated personal wardrobe.

## Real OnePlus 13R checks still required

- Camera app capture/cancel and multiple-gallery selection; deny the old-Android export permission on an Android 8–9 test device if that platform matters.
- Initial Google Play services model provisioning, cutout quality on real garments, and fallback with airplane mode / missing model. No successful ML segmentation on real clothing is claimed from a build alone.
- Brush erase/restore/undo, rotate while editing, large font settings, keyboard form access, dark mode, and landscape.
- Shuffle, manual swaps, saving/favorites, delete confirmations, app restart, offline operation, gallery export, and share targets installed on your phone.
- GitHub Actions execution/artifact availability: implementation commits were successfully pushed to main after switching unpublished commit authors to GitHub’s no-reply address to satisfy email privacy protection. Local build success is verified separately; GitHub API access is unavailable from this task, so no Actions run success is claimed.

## Current scope limits

- ML Kit is a beta subject detector and its Play services model may need an initial download. Automatic cutouts on a fresh never-connected phone are not guaranteed. Manual editing and keeping originals remain available offline.
- Saved outfits are immutable PNG snapshots, not an editable multi-piece project loader. Editing clothing affects new looks, not old saved snapshots.
- Wardrobe backup/restore is not implemented. Uninstall/clear data destroys the local collection. Export saves outfit images only.
- CI debug signing keys are temporary and can differ between workflow runs. Stable update signing is not configured; see README before replacing an installed APK.
- Device model is confirmed as OnePlus 13R; physical-device testing has not occurred.
