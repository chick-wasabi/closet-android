# Contributing

- Keep wardrobe processing local and preserve existing user data.
- Run `./gradlew testDebugUnitTest` and `./gradlew --max-workers=1 lintDebug` for relevant changes.
- For UI changes, inspect actual renders at narrow/large-text/dark/landscape sizes, then run `scripts/update_screenshots.py` and `--check`. Do not substitute marketing mockups for in-app screenshots.
- Public documentation covers installation, use, architecture, and contribution—not private conversations or personal device assumptions.
- Package builds only with the existing permanent signing identity. Preserve application ID and increase versionCode for native updates. Never silently generate a new distribution key.
- Use finite standard Compose animations that follow Android's duration setting. Keep text and touch targets usable with system font scaling.
- Use deterministic illustrated fixtures or properly licensed sample clothing in public screenshots, never personal wardrobe photos.
- Database changes require exported schemas and a tested non-destructive Room migration. Preserve the version-1 schema as the installed-data baseline.
- Keep style policy in the versioned JSON asset. Photo suggestions must stay editable and failure must leave manual saving available.
- Backup restore must validate all entries and image references before replacing live rows. Never extract archive paths directly into app storage.
- Keep `app/src/main/assets/glossary.md` identical to `GLOSSARY.md` for the offline glossary.
