# Validation guide

See [testing](TESTING.md) for logic tests, native Compose renders, lint, device checks, and signed APK verification.

Local build results do not establish that camera behavior, segmentation quality, sharing, or an installation update worked on a physical device. Generated screenshots are actual component renders with example data, not physical-device screenshots.

The screenshot manifest records a source fingerprint. Changing UI or screenshot fixtures requires rerendering and refreshing public images before committing. CI validates freshness without repository write access.

## Version 1.3 coverage

The host suite contains 41 app tests: 14 outfit-engine tests, 5 analysis tests, 2 native-image processing tests, 3 database/backup tests and 17 native Compose render/layout checks. The signing scripts have 4 additional policy checks. Serialized Android lint and device-test compilation are part of development validation.

On-device camera capture, first-time subject-model provisioning, real garment cutouts/labels, document-provider behavior and upgrade installation still require physical-phone testing. General photo labeling and mask statistics are suggestions; their accuracy is not guaranteed.
