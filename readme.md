# MobileSystems

English | [Deutsch](readme_ger.md)

This repository contains independent Android app projects (`A2_*`, `A3_*`, `A4_*`, `A5_*`, and later sections) and one shared Android library, `Shared`. Open an **app directory directly** in Android Studio, for example `A5_10_PeopleRetrofit`. Its Gradle build includes only that app and `Shared`; the repository root is not an aggregate build.

## Set up a fresh clone

Install the Android SDK and set `ANDROID_HOME` to its location. On a Mac with Android Studio's default SDK location:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

Use the actual SDK path on another computer. The builds need the SDK platform and build tools configured by the project. The Gradle wrapper and Java 21 toolchain are configured in the repository; the first build may download missing components.

`local.properties` is ignored by Git, including symbolic links with that name. Neither the root file nor app-local files are present after a fresh clone. The build scripts still work when `ANDROID_HOME` points to a valid SDK. For Android Studio, you can let the IDE create `local.properties` in each opened app, or create an app-local file containing `sdk.dir=/absolute/path/to/Android/sdk`. Older apps do not inherit a root-level `local.properties`. Do not put `sdk.dir` in `gradle.properties` or commit machine-specific SDK paths.

## Learning progression

The projects build on one another. Each step adds one main concept to the previous examples:

- `A2_01_Count`: introduce observable Compose state, state hoisting, and recomposition.
- `A2_02_Layout`: arrange content with `Column`, `Row`, `Box`, and `FlowRow`, then compare image scaling.
- `A2_03_TextField`: connect text input to stateholders, MVVM, and MVI.
- `A2_04_LazyColumn`: display keyed lists and connect list events to observed Room data.
- `A3_01_Material`: add a Material form, mapping, and validation.
- `A3_02_EffectHandling`: separate persistent UI state from one-time messages and errors.
- `A3_03_Navigation`: route navigation effects through Navigation 3.
- `A3_04_SwipeGestures`: distinguish swipe-to-open from swipe-to-confirm-delete.
- `A3_05_SwipeDeleteUndo`: add immediate visual removal, delayed deletion, undo, and restoration.
- `A4_01_ImagePicker`: select images and manage temporary or replaced files through save outcomes.
- `A5_01_PeopleRoom3`: persist people in Room 3 behind a repository.
- `A5_02_UsedCarsRoom3`: model sellers, cars, and test drives with Room relationships.
- `A5_10_PeopleRetrofit`: replace Room with a people web API while passing only image paths.
- `A5_11_PeopleImagesRetrofit`: add API-managed image upload and deletion.
- `A5_12_RetrofitNews`: combine read-only News API search with locally saved Room articles.
- `A5_13_UsedCars`: move the people, cars, and test-drive model from Room to Retrofit.

## Project guides

Each app project and the `Shared` library have an English and a German guide. Follow the app rows from Compose fundamentals through effects, navigation, images, Room, and web APIs:

| Project | Focus | Guides |
| --- | --- | --- |
| `A2_01_Count` | Counter state, state hoisting, and recomposition | [English](A2_01_Count/readme201.md) · [Deutsch](A2_01_Count/readme201_ger.md) |
| `A2_02_Layout` | `Column`, `Row`, `Box`, `FlowRow`, and images | [English](A2_02_Layout/readme202.md) · [Deutsch](A2_02_Layout/readme202_ger.md) |
| `A2_03_TextField` | Text input with stateholder, MVVM, and MVI variants | [English](A2_03_TextField/readme203.md) · [Deutsch](A2_03_TextField/readme203_ger.md) |
| `A2_04_LazyColumn` | Keyed person list, Room observation, and list events | [English](A2_04_LazyColumn/readme204.md) · [Deutsch](A2_04_LazyColumn/readme204_ger.md) |
| `A3_01_Material` | Material form, mapping, and validation | [English](A3_01_Material/readme301.md) · [Deutsch](A3_01_Material/readme301_ger.md) |
| `A3_02_EffectHandling` | One-time UI effects | [English](A3_02_EffectHandling/readme302.md) · [Deutsch](A3_02_EffectHandling/readme302_ger.md) |
| `A3_03_Navigation` | Navigation 3 and navigation effects | [English](A3_03_Navigation/readme303.md) · [Deutsch](A3_03_Navigation/readme303_ger.md) |
| `A3_04_SwipeGestures` | Swipe actions and delete confirmation | [English](A3_04_SwipeGestures/readme304.md) · [Deutsch](A3_04_SwipeGestures/readme304_ger.md) |
| `A3_05_SwipeDeleteUndo` | Visual removal and undo | [English](A3_05_SwipeDeleteUndo/readme305.md) · [Deutsch](A3_05_SwipeDeleteUndo/readme305_ger.md) |
| `A4_01_ImagePicker` | Image selection and file lifecycle | [English](A4_01_ImagePicker/readme401.md) · [Deutsch](A4_01_ImagePicker/readme401_ger.md) |
| `A5_01_PeopleRoom3` | People stored in Room 3 | [English](A5_01_PeopleRoom3/readme501.md) · [Deutsch](A5_01_PeopleRoom3/readme501_ger.md) |
| `A5_02_UsedCarsRoom3` | Sellers, cars, and test drives with Room relationships | [English](A5_02_UsedCarsRoom3/readme502.md) · [Deutsch](A5_02_UsedCarsRoom3/readme502_ger.md) |
| `A5_10_PeopleRetrofit` | People loaded through a web API; only image paths are stored | [English](A5_10_PeopleRetrofit/readme510.md) · [Deutsch](A5_10_PeopleRetrofit/readme510_ger.md) |
| `A5_11_PeopleImagesRetrofit` | People API with separate image upload and deletion endpoints | [English](A5_11_PeopleImagesRetrofit/readme511.md) · [Deutsch](A5_11_PeopleImagesRetrofit/readme511_ger.md) |
| `A5_12_RetrofitNews` | Read-only News API search; selected articles saved locally in Room | [English](A5_12_RetrofitNews/readme512.md) · [Deutsch](A5_12_RetrofitNews/readme512_ger.md) |
| `A5_13_UsedCars` | A5_02's people, cars, and test drives through Retrofit | [English](A5_13_UsedCars/readme513.md) · [Deutsch](A5_13_UsedCars/readme513_ger.md) |
| `Shared` (library) | Reusable UI, image, Room, network, and DI components | [English](Shared/readme.md) · [Deutsch](Shared/readme_ger.md) |

## Test layout

Tests use the normal Android source sets: `src/test/java` for local JVM tests and `src/androidTest/java` for tests requiring Android or Compose on a device. Test packages mirror production packages. Module-local helpers live in `de.rogallab.mobile.testing`.

- `A2_01_Count`, `A2_02_Layout`, `A2_03_TextField`, and `A2_04_LazyColumn`: local and instrumented starter tests; they do not yet assert each example's main behavior.
- `A3_01_Material`: mapping, validation, and basic ViewModel state.
- `A3_02_EffectHandling`: one-time `ShowMessage` and `ShowError` effects.
- `A3_03_Navigation`: navigation and back effects plus load failures. The app uses Material 3 `SnackbarHostState` directly, so it has no separate snackbar coordinator test.
- `A3_04_SwipeGestures`: ViewModel confirmation behavior and instrumented swipes in both directions.
- `A3_05_SwipeDeleteUndo`: ViewModel undo and restoration plus instrumented swipe and restored-list behavior.
- `A4_01_ImagePicker`: image and use-case behavior around successful and failed saves, plus navigation and swipe tests.
- `A5_01_PeopleRoom3`: repository, use-case, image, ViewModel, and swipe behavior.
- `A5_02_UsedCarsRoom3`: Room DAO and relationship tests, converters, repository behavior, and deletion flows.
- `A5_10_PeopleRetrofit`: remote mapping, image-path handling, use cases, navigation, and swipe behavior.
- `A5_11_PeopleImagesRetrofit`: image upload and deletion in the repository, mapping, use cases, and UI behavior.
- `A5_12_RetrofitNews`: both repositories and three feature ViewModels; the Android test is a starter example.
- `A5_13_UsedCars`: remote mapping, `Instant` serialization, image handling, and list ViewModels; there are currently no `src/androidTest` tests.
- `Shared`: local tests cover utilities, storage, image editing, `SnackbarController`, `EffectDelegate`, and visual removal. Instrumented tests cover Android-backed storage, database integration, image picking, and Compose components.

The apps target Android API 37. Android-related local tests using Robolectric 4.16.1 select supported test API 35 with `@Config(sdk = [35])`; this does not change the production target. `MainDispatcherRule` switches `Alog` to non-Android output so pure JVM tests do not call `android.util.Log`.

Tests need not be physically next to production files, but they must belong to a Gradle test source set. Larger projects can use Gradle `testFixtures` or a separate test-support module; these teaching projects keep their small helpers in each module.

### Build and test all apps

From the repository root, run the three scripts in order. Each script also works on its own and from another working directory.

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk" # adjust to your machine
./scripts/assemble.sh && \
./scripts/test.sh && \
./scripts/androidtest.sh
```

1. `assemble.sh` deletes each app's local Gradle configuration cache, prints its name, and runs `assembleDebug`.
2. `test.sh` prints each app's name and runs local JVM tests with `testDebugUnitTest`.
3. `androidtest.sh` prints each app's name and runs `connectedDebugAndroidTest` on a connected device or emulator. It marks apps without Android tests as skipped. It fails if an app has Android tests but Gradle produces no test results, even when Gradle exits successfully.

Each script stops at the first failure. `&&` starts the next phase only after the previous one succeeds. Only the per-app configuration cache is removed; Gradle's global cache of downloaded dependencies remains. The scripts use `--quiet --warning-mode=none` to hide routine Gradle messages while keeping failures visible. For diagnosis, run the affected app's Gradle task without those options.

The scripts iterate over app directories; run library-specific tests separately as described in the [Shared guide](Shared/readme.md).

If multiple devices are connected, select one explicitly, for example:

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/androidtest.sh
```

This also avoids an installation conflict if another device has the same application ID installed with a different signing certificate. Instrumented tests need a device or emulator; builds and local JVM tests do not.

To rerun one app with full Gradle output, use `./gradlew -p A3_05_SwipeDeleteUndo testDebugUnitTest` or `ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_05_SwipeDeleteUndo connectedDebugAndroidTest`. Local test reports are under `A3_05_SwipeDeleteUndo/build/reports/tests/testDebugUnitTest/`; Android test reports are under `A3_05_SwipeDeleteUndo/build/reports/androidTests/connected/debug/`.

The scripts build the apps one at a time. Gradle can execute tasks within a build in parallel according to `gradle.properties`. Concurrent app builds are not supported by this layout because they share the output directories of `Shared` and `build-logic`.

## Create and open another app

From the repository root:

```bash
./scripts/create-app-project.sh A6_01_Sensors
```

The name must match `A<section>_<two-digit number>_<Name>`, for example `A6_01_Sensors`. The directory and its derived application ID must be unused. The script creates a starter app with a manifest and `MainActivity`, copies the Gradle settings and app build file from `A2_02_Layout`, and links the shared Gradle files and wrapper. If a root `local.properties` exists, the new app gets an ignored link to it. The script does not copy another app's source code or change `Shared`.

Check the new project with:

```bash
cd A6_01_Sensors
./gradlew projects
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The `projects` output should show this app as the root project and only `:Shared` as a module. Open `MobileSystems/A6_01_Sensors` through **File → Open** in Android Studio and sync Gradle. Opening the repository root does not load all apps.

All apps use the common `gradle/libs.versions.toml` catalog and Kotlin convention plugins in `build-logic`: `mobile.android.application` for apps and `mobile.android.library` for `Shared`. Sync already open app projects after changing either. Put app-specific dependencies and BuildConfig fields in that app's `build.gradle.kts`.

`A5_12_RetrofitNews` accepts `NEWS_API_KEY` as a Gradle property or in its own `local.properties`; a root `local.properties` is a fallback. Without a key, its `BuildConfig.NEWS_API_KEY` is empty. Keep keys out of Git.

Commit new project files and the versioned Gradle symlinks. `local.properties` and links with that name stay local. On Windows, Git and Windows must support symbolic links for the shared wrapper and Gradle files.
