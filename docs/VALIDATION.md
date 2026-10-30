# Validation guide

See [testing](TESTING.md) for logic tests, native Compose renders, lint, device checks, and signed APK verification.

Local build results do not establish that camera behavior, segmentation quality, sharing, or an installation update worked on a physical device. Generated screenshots are actual component renders with example data, not physical-device screenshots.

The screenshot manifest records a source fingerprint. Changing UI or screenshot fixtures requires rerendering and refreshing public images before committing. CI validates freshness without repository write access.
