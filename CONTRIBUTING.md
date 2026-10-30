# Contributing

- Keep wardrobe processing local and preserve existing user data.
- Run `./gradlew testDebugUnitTest lintDebug` for relevant changes.
- For UI changes, inspect actual renders at narrow/large-text/dark/landscape sizes, then run `scripts/update_screenshots.py` and `--check`. Do not substitute marketing mockups for in-app screenshots.
- Public documentation covers installation, use, architecture, and contribution—not private conversations or personal device assumptions.
- Package builds only with the existing permanent signing identity. Preserve application ID and increase versionCode for native updates. Never silently generate a new distribution key.
- Use finite standard Compose animations that follow Android's duration setting. Keep text and touch targets usable with system font scaling.
- Use deterministic illustrated fixtures or properly licensed sample clothing in public screenshots, never personal wardrobe photos.
