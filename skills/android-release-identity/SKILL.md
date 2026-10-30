---
name: android-release-identity
description: Establish permanent Android signing before the first APK and preserve updates across debug and release.
---

For each new Android application, establish its application ID, one private long-lived signing identity, a secure external backup, a CI secret, and a public certificate pin before its first distribution. Use that identity for all distributed debug and release APKs of that app. Do not share private keys across unrelated apps by default.

Never silently create a runner debug key, regenerate a missing identity, rely only on a cache, commit private signing material, or disable verification. Missing/mismatched keys must fail packaging. Keep versionCode increasing for native updates. Verify actual APK certificates before artifact upload and test an update installation when a device is available.

Changing an existing identity requires the old key and a supported migration/rotation plan. A public certificate cannot reconstruct a lost private key. Distinguish Play app-signing keys from upload keys.
