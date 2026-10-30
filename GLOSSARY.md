# Closet glossary

## The app and your data

| Term | In plain words |
| --- | --- |
| Offline / on-device | Works on the phone without sending clothing photos to a server. ML Kit may first need its model delivered by Google Play services. |
| Native Android | An app written for Android itself, rather than a website inside an app. |
| APK | The installation file you open on Android. |
| Debug build | A development version with debugging enabled and a testing signature. |
| Signing key / signature | The identity Android checks before allowing one APK to update another. A different signature generally requires uninstalling first. |
| Private app storage | Files only this app can normally access; uninstalling deletes them. |
| Database | Structured storage for item details and saved looks. |
| Room | Android’s library for safely storing and querying a local SQLite database. |
| SQLite | The small database engine inside Android. |
| Entity / table / row | A type of stored record, its collection, and one saved record. |
| Primary key / UUID | A unique ID used to distinguish each item or outfit. UUID generates IDs without a server. |
| DAO / query | A small interface containing operations that read or change the database. |
| Flow / StateFlow | A stream that updates the screen when database contents change. StateFlow keeps the latest value. |
| Snapshot | A saved image of a complete outfit, independent of later clothing edits. |
| PNG / transparency / alpha | An image format that supports invisible pixels; alpha measures how visible each pixel is. |
| Bitmap / pixel / ARGB | An image held in memory, its tiny colored dots, and their alpha/red/green/blue channels. |
| Canvas / compositing | A surface for drawing; compositing combines several images into one. |
| Mask / segmentation / cutout | A separation between subject and background; a cutout retains the subject with transparent surroundings. |
| ML Kit Subject Segmentation | Google’s on-device subject-detection library used to attempt automatic background removal. It is a beta library, so behavior can change. |
| Machine learning / model / inference | Software trained from examples, the learned detector, and using it to process a new photo. Closet does not train models. |
| Google Play services | Google’s phone service that can supply ML Kit’s model. Not all Android phones have it. |
| Brush / restore / undo | Painting pixels away, painting the original back, and reversing recent actions. |
| EXIF / orientation | Camera metadata including how a photo should be rotated or mirrored. ExifInterface reads it on older Android versions. |
| ImageDecoder / BitmapFactory | Android tools that read image files into memory. |
| Downsampling / bounded import | Reducing photo dimensions to avoid excessive phone memory use. |
| Shader / PorterDuff | Android drawing tools used to restore original pixels or clear their alpha. |
| Color matching / rule-based generator | Local code that scores compatible clothing using editable rules, without an online AI service. |
| Formality / season / neutral color | How dressy an item is, suitable weather, and a color that is easy to combine. |
| Weight / probability / jitter | A rule’s importance, chance of including an optional piece, and small randomness to vary outfits. |
| JSON / asset | A readable structured text format and a file packaged inside the APK. |
| Interface / extension boundary | A code agreement where a different suggestion system could be connected later. It does not implement AI. |

## Screens and permissions

| Term | In plain words |
| --- | --- |
| Kotlin | The language used to write Closet. |
| Jetpack Compose / composable | Android’s modern screen-building library and a function that describes part of a screen. |
| Material 3 | Android’s design components: buttons, dialogs, navigation, colors, and typography. |
| Theme / dark mode / typography | Shared visual styling, a dark palette, and choices for readable text. |
| Adaptive / responsive layout / dp | Layout that adjusts to available width; dp is Android’s density-independent size unit. |
| Scaffold / snackbar / dialog | A screen frame, a short status message, and a focused pop-up interaction. |
| Lazy grid / lazy list | Scrolling layouts that create visible cells as needed instead of loading every cell. |
| State / recomposition | Current screen values and Compose redrawing after they change. |
| ViewModel / lifecycle | Screen logic that survives rotation; lifecycle describes when a screen is active or gone. Unfinished edits do not survive the OS killing the app. |
| Coroutine / suspend / dispatcher | A lightweight background task, a function that can wait without freezing the screen, and the choice of worker for a task. |
| IO / main thread | Background file/database work versus the thread responsible for screen interaction. |
| Timeout / cancellation | Stopping an operation that takes too long or is no longer needed. |
| Coil / AsyncImage | A library and Compose element that load local images without blocking scrolling. |
| Activity / intent / activity result | An Android screen, a request to another app, and its returned result. |
| System photo picker / document picker | Android’s chooser granting access to only selected photos. |
| URI / FileProvider | A reference to content and a safe way to temporarily share an app-owned file. |
| Permission / scoped storage / MediaStore | Android access controls, restricted shared-file access, and the gallery’s media storage API. |
| API | A defined way for code to use a system or library. No paid web API is used. |
| Manifest | Android’s declaration of the app’s screens, permissions, and providers. |
| Cache | Temporary files Android can clear; used for camera captures and share copies. |
| Backup | A separate copy for recovery. Closet disables automatic app backup and currently has no full wardrobe backup/restore feature. |
| Accessibility / semantics | Labels, readable scaling, and screen information that help assistive tools and users. |

## The lookbook design

| Term | In plain words |
| --- | --- |
| Condensed type / masthead | Narrow display letters and a large page heading, used to give Closet its fashion-poster character. |
| Contact sheet / registration grid | A neatly arranged set of image frames and fine guide lines behind cutouts. These are visual treatments, not extra wardrobe data. |
| Design token / color role | A shared size, font, or color choice used consistently across screens. A color role identifies what the color does, such as text or a selected button. |
| Robolectric / native graphics | An Android simulation running on the development computer, with Android's drawing code used for realistic component rendering. It is not a physical phone. |
| Roborazzi / render check | A tool that captures Android UI images for review. Closet records screenshots; it does not yet compare them against approved reference images. |
| Fixture | Synthetic test data, such as the drawn shirt in review screenshots. It is not shipped as a real wardrobe item. |

## Building and testing

| Term | In plain words |
| --- | --- |
| Repository / Git / GitHub | The project folder, its version history tool, and the service hosting it. |
| Commit / branch / main / push | A recorded change, a line of work, the primary branch, and sending commits to GitHub. |
| GitHub Actions / workflow / CI | Automated checks on GitHub, their instruction file, and automatically building/testing changes. |
| Artifact / retention | A downloadable workflow output and how long GitHub keeps it. |
| Runner / Node.js / actionlint | The machine executing a workflow, the JavaScript runtime used by workflow actions (not the phone app), and a checker for workflow errors. |
| YAML | The text format used by GitHub’s workflow file. |
| Gradle / wrapper | Android’s build tool and the committed launcher that downloads the exact required Gradle version. |
| JDK / JVM / bytecode | The Java development tools, the machine that runs Java tools/tests, and compiled instructions. |
| Android SDK / AGP | Android’s development tools and the Android Gradle Plugin connecting them to Gradle. |
| minSdk / targetSdk / compileSdk | Oldest supported Android version, version whose behavior the app opts into, and version used to compile. |
| Dependency / Maven / BOM | A library used by the app, a library repository format, and a coordinated list of library versions. |
| KSP | Kotlin Symbol Processing, a newer way to generate supporting code; Room can use it as a future alternative to kapt. |
| kapt / annotation processor | A compiler helper that generates Room’s database implementation from declarations. |
| TLS / CA / checksum | Secure download connections, trusted certificate authorities, and a digest used to verify downloaded bytes. |
| Proxy / environment variable | The cloud machine’s network gateway and a setting supplied to a command. No credentials belong in committed files. |
| Unit test / JUnit | A small automated logic check and the framework running it. |
| Instrumentation test / AndroidJUnitRunner | A test running on Android and the component that launches those tests. |
| Emulator / ADB | A virtual Android phone and Android Debug Bridge, the command tool communicating with a phone or emulator. |
| Espresso / Compose test rule | Android UI testing tools that drive clicks and inspect screen elements. |
| Smoke test / regression | A quick important-flow check and a previously working behavior breaking after a change. |
| Lint / static analysis | Automated inspection for likely Android correctness, usability, or compatibility problems. |
| Build / compile / package | Turn source code into executable instructions and combine them with resources into an APK. |
| Release | A distributed version intended for users. An uploaded debug artifact is not a Play Store release. |
