# Closet backups

Use Settings → Export backup and choose a private destination in Android's file picker. A ZIP contains all saved clothing PNGs, original photos, outfit images, a portable logical Room database snapshot (`database.json`), settings, wear history and pair preferences. It does not contain the app's signing key or an APK. An unsaved review batch is not part of the saved closet.

Backups are not encrypted. A storage provider selected in the picker may sync files; choose its destination deliberately. Regular exports protect against uninstalling, clearing storage, or losing a phone. Keep a second copy outside app storage.

Restore replaces the current closet after confirmation. It validates the archive version, allowed filenames, duplicate entries, referenced PNGs and image bounds before replacing database records in a transaction. Images are staged under private storage and assigned fresh paths. Failed validation leaves the saved closet unchanged. The import limit is 1 GB uncompressed, 32 MB per entry and 20,000 entries. The current format is version 2.

Export the current closet before restoring a different backup. Outfit PNG sharing is useful for pictures but does not preserve editable wardrobe records.
