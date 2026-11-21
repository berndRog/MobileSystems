# Shared

English | [Deutsch](readme_ger.md)

## Purpose and integration

`Shared` is the reusable Android library included as `:Shared` by each independent app project. Infrastructure, media services, and reusable Compose components live here; app-specific domain rules, repositories, screens, and webservice interfaces remain in the apps.

Open an **app directory** in Android Studio to work with the library. Shared has no launcher and no standalone Gradle settings. Each app's settings include `../Shared` and the common `build-logic` build; the repository root is not an aggregate app build.

Apps declare:

~~~kotlin
implementation(project(":Shared"))
~~~

The library uses the `mobile.android.library` convention plugin from [build-logic](../build-logic/), the common [version catalog](../gradle/libs.versions.toml), and the dependencies in [build.gradle.kts](build.gradle.kts). These include Compose, Navigation 3, Room 3 with bundled SQLite, Koin, Coil, Media3, Retrofit/OkHttp, and Kotlin serialization.

## Main building blocks

Paths below are relative to `src/main/java/de/rogallab/mobile/shared/`.

| Area | Reusable code and responsibility |
| --- | --- |
| UI effects | [ui/effects](src/main/java/de/rogallab/mobile/shared/ui/effects/): `EffectDelegate`, `IEffectSource`, `EffectHandler`, and `SnackbarController` support one-time events and snackbar handling. |
| Compose components | [ui/components](src/main/java/de/rogallab/mobile/shared/ui/components/): `SwipeCard` handles gestures while callers provide card content; `InputValueString` supplies reusable text input. `ScrollToItemIfNotVisible` in `ui/lists` helps reveal restored list items. |
| Temporary list removal | [ui/removal](src/main/java/de/rogallab/mobile/shared/ui/removal/): `IVisualRemoval` and `VisualRemovalDelegate` track hidden and pending items for Undo without accessing a repository. |
| Images and external camera | [ui/images](src/main/java/de/rogallab/mobile/shared/ui/images/): single/multiple gallery pickers, `CameraPickerHandler`, `ImageSelection`, `ImageRenderer`, and `IImageEdit`/`ImageEdit`. |
| Image storage | [data/local/io](src/main/java/de/rogallab/mobile/shared/data/local/io/): `ImageFileStorage` stores app-private images; `ImageMediaStore` manages images in shared media storage. Interfaces and IO types live in `domain/images`. |
| Video | `VideoPickerHandlerSingle` selects a video URI; `VideoPlayer` renders it with Media3. `IVideoMediaStore`/`VideoMediaStore` manage pending, published, and deleted MediaStore videos. |
| Camera permission UI | [ui/permissions](src/main/java/de/rogallab/mobile/shared/ui/permissions/): `CameraPermissionHandler`, `ButtonStepPermission`, and `PermissionReport` provide reusable permission-related UI. |
| Persistence | [data/local](src/main/java/de/rogallab/mobile/shared/data/local/): `AppDatabasePerson`, `IPersonDao`, `PersonDto`, converters, and configurable Room 3 database modules. |
| Networking | [data/network](src/main/java/de/rogallab/mobile/shared/data/network/): connection checking, an interceptor, exception types, and `NetworkExceptionMapper`; `networkModule` assembles Retrofit, OkHttp, and JSON conversion. |
| Utilities and lifecycle logging | [domain/utilities](src/main/java/de/rogallab/mobile/shared/domain/utilities/): string providers, logging, input sanitizers, UUID conversion, result and image-model helpers. `BaseActivity` in `ui` logs activity lifecycle callbacks. |

## Select the required Koin modules

The [di](src/main/java/de/rogallab/mobile/shared/di/) modules are opt-in. Configure Koin with the app's Android context and register only the modules needed by its features:

| Factory | What it registers |
| --- | --- |
| `utilitiesModule()` | `IStringProvider` backed by Android string resources. |
| `databaseModulePerson(databaseName, ioDispatcher)` | The shared `AppDatabasePerson` and its `IPersonDao`. The dispatcher defaults to `Dispatchers.IO`. |
| `databaseModule<TDatabase>(databaseName, queryCoroutineContext)` | An app-selected Room database using `BundledSQLiteDriver`. Query context defaults to `Dispatchers.IO`; app-specific DAO bindings remain in the app. |
| `imageStorageModule(directoryName, ioDispatcher)` | `IImageFileStorage`, `IImageMediaStore`, **`IVideoMediaStore`**, and `IImageEdit`. The image IO dispatcher defaults to `Dispatchers.IO`. |
| `networkModule(baseUrl, isDebug)` | Connection checker/interceptor, exception mapper, logging, OkHttp, `Json`, and Retrofit. |

Choose the database module appropriate to the app's model rather than registering overlapping database definitions. API interfaces and their Retrofit service bindings belong in the app.

The network module uses Kotlin serialization, ignores unknown JSON keys, and configures timeouts of 10 seconds for connection, 30 seconds each for reads/writes, and 60 seconds for a complete call. HTTP logging is BASIC in debug mode and NONE otherwise.

## Images, camera, and video

Gallery pickers return selected content URIs. `CameraPickerHandler` instead launches an **external camera app** through `ActivityResultContracts.TakePicture`. It prepares an app-private destination file, gives the camera a FileProvider URI, and returns the confirmed internal file path through `onPhotoStored`. Cancellation/failure triggers cleanup of the provisional file; errors are forwarded through `onError`. The UI receives `CameraPickerActions` with `takePhoto` and `isBusy`.

The library [manifest](src/main/AndroidManifest.xml) declares a non-exported FileProvider with authority `${applicationId}.fileprovider` and temporary URI grants. Its [path configuration](src/main/res/xml/image_file_paths.xml) exposes `filesDir`; `ImageFileStorage` creates and validates images inside its configured subdirectory. URI and internal file path refer to the same image but serve different purposes.

`CameraPermissionHandler` supplies a permission-request action and invokes `onGranted` after CAMERA permission is available. It also provides rationale/settings dialogs. App-specific capture flows, feature orchestration, and required manifest permissions remain the responsibility of the consuming app; the external-camera picker is separate from an in-app CameraX implementation.

`VideoPickerHandlerSingle` uses the system visual-media picker restricted to videos. `VideoPlayer(uri, onClose, onSelectOther)` creates a player per URI, prepares and starts playback, offers play/pause and selection/close actions, pauses on `ON_STOP`, and releases the player when it leaves the composition. It does not persist playback position across recreation.

For video storage, `createPendingVideo` creates a hidden entry in the Movies collection and returns a `PendingVideo` with a writable descriptor. Close the descriptor when writing finishes, then publish a completed video with `publishVideo`; use `deleteVideoFromMediaStore` for cleanup. Recording itself is managed by the app.

## Tests and commands

Local tests under [src/test/java](src/test/java/) cover utilities, DTOs/converters, image storage/editing, DI modules, lifecycle logging, effects, snackbars, and visual removal. Android-backed local tests use Robolectric. Instrumented tests under [src/androidTest/java](src/androidTest/java/) cover storage, Room/DAO integration, picker handlers/results, and Compose components.

Run Shared tests through an app's Gradle settings, for example from the repository root:

~~~bash
./gradlew -p A3_04_SwipeGestures :Shared:testDebugUnitTest
ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
~~~

Windows PowerShell:

~~~powershell
.\gradlew.bat -p A3_04_SwipeGestures :Shared:testDebugUnitTest
$env:ANDROID_SERIAL = "emulator-5554"
.\gradlew.bat -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
Remove-Item Env:ANDROID_SERIAL
~~~

Builds/tests require the project's JDK and Android SDK setup; device tests additionally need a running emulator or authorized device. `ANDROID_SERIAL` selects a device when several are connected.

The repository test scripts **already run Shared once through the first app**, then test the apps. You do not need another explicit Shared run when using those scripts:

~~~bash
./scripts/testDebugUnitTest.sh
./scripts/connectedDebugAndroidTest.sh
# Build, local tests, and device tests in order, with a timing log:
./scripts/runDebugChecks.sh
~~~

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\testDebugUnitTest.ps1
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
~~~

See the [Bash script guide](../scripts/Readme.md) and [PowerShell script guide](../scripts_powershell/README.md) for prerequisites, failure behavior, and timing logs. Shared reports normally appear under `Shared/build/reports/tests/testDebugUnitTest/` and `Shared/build/reports/androidTests/connected/debug/`.

After changing shared behavior, check both the library tests and an app that consumes the affected feature. Run app builds sequentially: they share output directories in Shared and build-logic. The test descriptions above describe existing coverage, not a guarantee that every media or permission flow has a dedicated test.
