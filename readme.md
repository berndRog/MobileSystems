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

## Build and test all apps

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

If multiple devices are connected, select one explicitly, for example:

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/androidtest.sh
```

This also avoids an installation conflict if another device has the same application ID installed with a different signing certificate. Instrumented tests need a device or emulator; builds and local JVM tests do not.

To rerun one app with full Gradle output, use `./gradlew -p A3_05_SwipeDeleteUndo testDebugUnitTest` or `ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_05_SwipeDeleteUndo connectedDebugAndroidTest`. Local test reports are under `A3_05_SwipeDeleteUndo/build/reports/tests/testDebugUnitTest/`; Android test reports are under `A3_05_SwipeDeleteUndo/build/reports/androidTests/connected/debug/`.

The scripts build the apps one at a time. Gradle can execute tasks within a build in parallel according to `gradle.properties`. Concurrent app builds are not supported by this layout because they share the output directories of `Shared` and `build-logic`.

## Test layout and learning progression

Tests use the normal Android source sets: `src/test/java` for local JVM tests and `src/androidTest/java` for tests requiring Android or Compose on a device. Test packages mirror production packages. Module-local helpers live in `de.rogallab.mobile.testing`. Local tests cover mappings, validators, ViewModels, effects, and repository fakes; instrumented tests cover behavior such as swipe gestures.

- `A3_01_Material`: mapping, validation, and basic ViewModel state.
- `A3_02_EffectHandling`: one-time `ShowMessage`/`ShowError` effects and prepared back navigation.
- `A3_03_Navigation`: `NavigateTo`/`NavigateBack` effects and Navigation 3. The app uses Material 3 `SnackbarHostState` directly, so no separate snackbar coordinator test is needed.
- `A3_04_SwipeGestures`: StartToEnd opens an existing person in detail mode. EndToStart asks for confirmation through an action snackbar; only its delete action changes the repository. Instrumented tests cover both directions. Undo is introduced later.
- `A3_05_SwipeDeleteUndo`: adds immediate visual removal, undo, delayed repository commit, restoration after commit errors, and scrolling a restored item into view. Tests cover restore and undo.
- `A4_01_ImagePicker`: adds image selection, image-path state, and the lifetime of temporary and replaced images, including successful and failed saves.
- `Shared`: tests reusable `SnackbarController`, `EffectDelegate`, and `SwipeCard` behavior. `SwipeCard` handles gestures while each app supplies its card content. Local snackbar tests cover direct message/error calls and the `performAction`/`dismiss` results.

The apps target Android API 37. Android-related local tests using Robolectric 4.16.1 select supported test API 35 with `@Config(sdk = [35])`; this does not change the production target. `MainDispatcherRule` switches `Alog` to non-Android output so pure JVM tests do not call `android.util.Log`.

Tests need not be physically next to production files, but they must belong to a Gradle test source set. Larger projects can use Gradle `testFixtures` or a separate test-support module; these teaching projects keep their small helpers in each module.

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
