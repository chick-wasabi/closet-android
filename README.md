# Closet

A personal, local wardrobe for Android 8.0 and newer. Add your clothes, refine transparent cutouts, and make outfits from what you actually own. Native Kotlin, Jetpack Compose, Material 3, Room; minSdk 26, targetSdk 36 (Android 16).

Designed for the OnePlus 13R (1264 × 2780, 6.78-inch display), with adaptive grids for other phones and landscape.

No accounts, server, ads, subscriptions, paid API, or AI service. Wardrobe records and photos live in this app’s private storage. Backup is disabled. Uninstalling or clearing app storage deletes your wardrobe; exported PNGs are your portable copies. Keep your original photos elsewhere if you need a backup.

## Visual design

Closet 1.1 uses a cobalt, black, and cool-white fashion lookbook: condensed headings, a contact-sheet wardrobe grid, a framed studio canvas, and a dedicated cutout lab. Dark mode follows the same design. See [design direction and reviewed screens](docs/DESIGN_DIRECTION.md). All fonts and illustrations work offline. This native UI update requires a new APK; the authorized push workflow builds it automatically.

## Get the APK

1. Open [Actions → Android debug APK](https://github.com/chick-wasabi/closet-android/actions/workflows/android.yml) while signed into GitHub with access to this repository.
2. Select the latest **successful** run for `main`. Every push builds automatically; you can also choose **Run workflow** → `main`.
3. Scroll to **Artifacts**, download **Closet-debug**, and unzip the downloaded file. It contains `app-debug.apk`. GitHub artifacts expire after the repository’s configured retention period; rerun the workflow if needed.
4. Transfer the APK to your Android phone or download it on the phone. Open it from Files / Downloads. Allow **Install unknown apps** for that browser or file manager when Android asks, then tap **Install**. You can switch that permission off afterward.
5. Open **Closet**. This is a debug build for personal testing, not a Play Store release. GitHub-hosted runners create temporary debug signing keys; later CI APKs may have a different signature and refuse to update an installed copy. Do not uninstall a populated wardrobe casually: export wanted outfits first. A stable private signing key is a separate future release setup.

## First five minutes

1. Add one top, one bottom, and shoes using **Add piece → Choose photos** (multiple photos supported, up to 30 at a time). Review and save each photo separately; classify each item correctly.
2. Try **Take photo** too. A plain contrasting background, good light, and one garment per photo work best. ML Kit is a subject detector and may struggle with flat-lay garments, hangers, fine straps, or multiple subjects.
3. Inspect the cutout on the checkerboard. Turn on **Brush**, choose **Erase** or **Restore**, adjust brush size, and draw. **Undo** retains up to five strokes/actions. **Keep original** restores the entire original photo; **Reset cutout** returns to the automatic result. Turn Brush off to scroll over the image. Save your metadata and piece.
4. In **Style**, choose season and formality, then **Shuffle**. You need a matching top, bottom, and shoes. Optional hats and jackets may join. **Swap** changes one slot; manual swaps intentionally override filters. Save or favorite the look.
5. In **Saved**, open a look, **Save to gallery** or **Share PNG**. The exported image has a transparent background and contains the entire stacked outfit. Verify it in your gallery, then turn on airplane mode, restart Closet, and repeat search, editing, shuffle, save, and export.

Search matches names, categories, colors, formality, and season. Tap a closet card to edit metadata/fix its cutout or delete it. Saved looks are independent image snapshots, so deleting a piece does not damage a saved image. Favorite filtering and outfit deletion are in Saved.

## Offline behavior and permissions

The requested **ML Kit Subject Segmentation** is an on-device, Google Play services delivered model. A compatible Google Play services installation and an initial model download may be required. This is a limitation of that library: automatic cutouts cannot be guaranteed on a fresh, never-connected phone or a phone without Google Play services. Closet times out after 30 seconds and offers the original plus manual erase/restore. Once provisioned, the model performs segmentation locally; the core wardrobe and outfit tools work without it.

Closet removes the Internet permission inherited from dependencies. It has no remote image loader usage, backend, or account. Google Play services manages its own model download independently; choosing Share explicitly hands your exported PNG to the app you choose.

Camera capture uses your existing camera application and a temporary FileProvider URI, so Closet does not request camera access itself. The system photo picker (or Android document-picker fallback) grants access only to selected photos; no blanket photo-library permission is needed. On Android 10+, export uses scoped MediaStore storage with no storage permission. On Android 8–9 only, Gallery export asks for write permission; denial leaves Share available. Android accessibility font sizes and system dark mode are supported.

## Develop

Use Android Studio with JDK 21, Android SDK Platform 36 and Build Tools 35.0.0. The committed wrapper downloads checksum-verified Gradle 8.13; dependencies come from Google Maven and Maven Central. Android Gradle Plugin 8.13.2, Kotlin 2.2.21, Compose BOM 2025.10.01. Java/Kotlin bytecode targets 17.

```sh
# In this repository (SDK location can also be set using local.properties)
export ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug
# With a connected phone / emulator:
./gradlew connectedDebugAndroidTest
```

The APK appears at `app/build/outputs/apk/debug/app-debug.apk`. Unit-test reports are under `app/build/reports/tests/`; lint reports under `app/build/reports/`. CI uploads both APK and validation reports. The workflow pins Ubuntu 24.04 and explicitly installs SDK 36 / Build Tools 35.0.0 using an explicit Android setup action that installs the SDK manager and adds it to PATH; it avoids the removed legacy `tools` SDK package. Unit tests exercise outfit completeness, filters, color scoring, ordering, and editable rules. Host-side Compose render checks also produce light/dark, narrow-phone, large-text, and landscape images under `app/build/outputs/design-review/`, uploaded with validation reports. Device tests cover navigation, persistence, and stacked PNG pixel/alpha correctness. See [validation notes](docs/VALIDATION.md) for what was actually executed and what still needs a real phone.

## Project map

| Path | Purpose |
| --- | --- |
| `app/src/main/java/dev/closet/MainActivity.kt` | Android entry point |
| `ui/ClosetApp.kt` | Closet grid, studio, saved looks, camera/picker/export controls |
| `ui/ItemEditor.kt` | Metadata form and erase/restore bitmap brush |
| `ui/ClosetViewModel.kt` | Screen state, import queue, safe background tasks, persistence |
| `data/ClosetDatabase.kt` | Room records and database queries |
| `imaging/ImageStore.kt` | Oriented/bounded imports, segmentation, PNG storage, compositing, gallery/share |
| `domain/OutfitEngine.kt` | Local matching and the future suggestion-provider interface |
| `app/src/main/assets/style_rules.json` | Editable style policy |
| `app/src/test/` | JVM rule tests |
| `app/src/androidTest/` | Android integration/smoke tests |
| `.github/workflows/android.yml` | Build/test/lint/APK artifacts for every push |
| `GLOSSARY.md` | Plain-language guide to terms used here |

Paths starting `ui/`, `data/`, `imaging/`, and `domain/` in the table are inside `app/src/main/java/dev/closet/`.

## Edit style rules

Change `app/src/main/assets/style_rules.json`, then rebuild/reinstall the APK. `neutralColors` match any color; `colorPairs` are symmetric pairs. Weights reward color, formality, and seasonal compatibility. `allowAdjacentFormality` rewards neighboring casual/smart/formal levels. Optional hat/jacket probabilities range from 0 to 1. Version 1 fixes `stackOrder` to hat, top, jacket, bottom, shoes; jackets overlay tops. Unsupported policy versions/order fail early rather than silently misrendering.

Season/formality selections are hard filters. Within that pool, the generator picks a random top, scores candidates against selected pieces, and uses a little randomness among good matches. Pajamas, accessories, and other items can be catalogued but are not automatically stacked into the four-slot day outfit. `OutfitSuggestionProvider` is the extension boundary for a future suggestion feature; no AI is implemented.
