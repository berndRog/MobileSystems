# Shared

English | [Deutsch](readme_ger.md)

## Purpose and use

`Shared` is the reusable Android library included as `:Shared` by each independent app project. It supplies infrastructure and UI building blocks; each app keeps its own domain rules, repositories, screens, and API interfaces. Apps add `implementation(project(":Shared"))` and select the Koin modules they need. Open an app directory in Android Studio to work with the library; `Shared` has no standalone app or launcher.

## Main building blocks

| Area | Reusable code |
| --- | --- |
| UI state and effects | `EffectDelegate`, `IEffectSource`, `EffectHandler`, and `SnackbarController` deliver one-time UI events. |
| Compose components | `SwipeCard` handles gestures while callers provide card content. `InputValueString` handles reusable text input; `ScrollToItemIfNotVisible` helps reveal a restored list item. |
| Temporary list removal | `IVisualRemoval` and `VisualRemovalDelegate` track hidden and pending items for Undo without accessing a repository. |
| Images | Picker handlers, `IImageEdit`/`ImageEdit`, file storage, MediaStore access, and image rendering support local selection and cleanup. |
| Persistence | `AppDatabasePerson`, `IPersonDao`, `PersonDto`, converters, and the generic Room database module support apps that use local Room data. |
| Networking | `networkModule` assembles Retrofit, OkHttp, JSON conversion, connection checks, timeouts, logging, and `NetworkExceptionMapper`. App-specific webservice interfaces remain in the apps. |
| Utilities | String resources/provider, logging, input sanitizers, UUID conversion, and image-model helpers. |

The Koin modules are opt-in: `utilitiesModule()`, `databaseModulePerson(...)` or `databaseModule(...)`, `imageStorageModule(...)`, and `networkModule(baseUrl, isDebug)`. An app registers only the modules required by its features. For example, A3_04 uses the shared swipe and effect components, A4_01 adds image services, and the Retrofit examples use the shared network stack.

## Tests

Local JVM tests under `src/test/java` cover utilities, DTOs and converters, image storage/editing, DI modules, effects, snackbars, and visual removal. Instrumented tests under `src/androidTest/java` cover Android-backed storage, Room/DAO integration, picker handlers, and Compose components. Instrumented tests need an emulator or device.

The repository scripts iterate over **app** projects, so run the library's tests explicitly from an app Gradle build, for example from the repository root:

```bash
./gradlew -p A3_04_SwipeGestures :Shared:testDebugUnitTest
./gradlew -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
```

`Shared` is included by the selected app's `settings.gradle.kts`; the second command needs a connected device. A change to shared behavior should be checked in the library and in an app that uses it.
