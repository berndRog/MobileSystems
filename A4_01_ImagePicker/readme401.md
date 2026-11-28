# A4_01_ImagePicker

## Purpose and prerequisites

**A4_01_ImagePicker builds on A3_04_SwipeGestures** and adds selecting a photo from the gallery, taking a photo with the camera, displaying it, and removing or replacing it. A second focus is the **introduction of use cases in the Domain layer**: operations that combine person persistence with image management are extracted from the ViewModel. The list (`PeopleAdapter`/`PeopleScreen`) retains swipe-to-detail and deletion with Snackbar confirmation. The form (`PersonAdapter`/`PersonScreen`) gains image actions. A3_05's Undo mechanism is a separate extension.

Prerequisites are Compose, state hoisting, intents, ViewModel state, effects, Navigation 3, and the confirmed-delete workflow from A3_04.

## Learning objectives

After studying the module, students should be able to:

- Integrate the Android Photo Picker and an external camera through separate Activity Result handlers.
- Distinguish a gallery content URI, a camera FileProvider URI, and an internal image path.
- Keep Android picker operations in the adapter/handlers while the screen receives values and callbacks.
- Explain the responsibilities of `IImageFileStorage`, `IImageEdit`, the ViewModel, and the person use cases.
- Introduce use cases in `domain/usecases`, invoke them through `suspend operator fun invoke()`, and distinguish application-operation coordination from UI-state management and repository access.
- Preserve original images until saving succeeds and discard unsaved images on Cancel.
- Trace image state to display, and image errors from the ViewModel through effects to a Snackbar.

## Two concrete image workflows

| Source | Handler in `Shared` | Processing |
| --- | --- | --- |
| Gallery | `GalleryPickerHandlerSingle` | Photo Picker → content URI → `GalleryImageSelected` intent → import through ImageEditDelegate |
| Camera | `CameraPickerHandler` | Prepare target file/URI → camera → confirm file → `CameraImageTaken` intent with internal path |

**Gallery:** `PickVisualMedia` selects one image. Cancelling the picker produces no selection event. The ViewModel delegates import to `IImageEditDelegate.importGallery()`. The delegate copies the image through file storage and returns the changed selection as `Result<ImageChange>`. The app stores its own image path instead of the picker URI.

**Camera:** `TakePicture` needs a writable destination before launch and returns a Boolean result. The handler creates a provisional file and passes its FileProvider URI to the external camera. It remembers the associated internal path, confirms a successful capture, and attempts cleanup on cancellation or failure. The FileProvider is declared in `Shared`.

Both paths then converge: **gallery URI/camera path → ImageEditDelegate → `ImageChange` → `PersonUiState.person.imagePath` → image display**.

## Didactic approach

Start with the visible actions, then compare their different technical requirements. `PersonScreen` uses `ImageSelection` and receives `onSelectPhoto`, `onTakePhoto`, and `onRemovePhoto`. `PersonAdapter` connects these callbacks to the handlers or intents and observes state lifecycle-aware. `ImageRenderer` uses Coil's `AsyncImage` or a placeholder icon when no image exists.

Separate obtaining a file from managing its edit session. `IImageFileStorage` performs file operations; `ImageEdit` tracks original and selected images. `ImageEditDelegate` coordinates imports, camera acceptance, replacement, removal, and rejected results. The ViewModel needs neither file storage nor ImageEdit directly. It uses `ImageSelectionMode.REPLACE` and `maxImages = 1`.

| Action | Image-session behavior |
| --- | --- |
| Replace/remove image | Update selection; discard obsolete unsaved replacements, retain persisted originals |
| Save succeeds | Create/update use case writes the person, then calls `ImageEdit.commit()`; unused originals can be deleted |
| Save fails | Keep original and replacement available for retry or Cancel |
| Cancel/System Back | Call `discard()` to remove unsaved images before emitting `NavigateBack` |

## Introducing use cases in the Domain layer

A4_01 introduces `PersonUcCreate`, `PersonUcUpdate`, and `PersonUcDelete` in `domain/usecases`. Image handling makes each application operation involve several steps and dependencies. A use case bundles these steps into one operation with an explicit order:

| Use case | Coordinated operation |
| --- | --- |
| `PersonUcCreate` | `repository.create(person)` → on success, `imageEdit.commit()` |
| `PersonUcUpdate` | `repository.update(person)` → on success, `imageEdit.commit()` |
| `PersonUcDelete` | person ID → `repository.findById(id)` → delete the current person → on success, clean up its image |

The ViewModel processes intents, manages UI state, validates the form, selects the matching use case, and translates its result into effects. The use case coordinates the application operation; the repository provides person-data access. Dependencies are supplied through constructor injection. `suspend operator fun invoke(person)` lets the ViewModel call a use-case instance like a function, for example `_personUcUpdate(person)`.

The learning step is to move the combined persistence/image operation out of the ViewModel and make its ordering independently testable. A failed repository write does not commit the image session; a failed person deletion leaves the image intact. Cleanup failures after successful person deletion are logged.

Image-copy failures and camera errors become `ShowError` effects from the ViewModel and are displayed in a Snackbar. Successful saving emits `ShowMessage`, followed by `NavigateBack`. The image path remains state; the request for a message or navigation is an effect.

## Generalized effect handling

As in A3_02 and A3_04, both ViewModels use the `EffectDispatcher` with their existing `EffectDelegate`. `launchShowError` and `launchEmit` start delivery from intents; `showError`, `showMessage`, and `emit` stay in the coroutine of the operation already running. The save confirmation is sent before back navigation. Image operations and Save retain their `UNDISPATCHED` start, guards, and completion handlers.

The dispatcher resolves ordinary message resources and logs their failures. Camera rejection failures already logged by Shared are forwarded without a Throwable to avoid a second log entry. Load failures remain in UiState; the delete confirmation text and action label remain in the ViewModel. Image import, commit, discard, and cleanup retain their responsibilities and lifetimes.

## Boundary with Chapter 5

ViewModels and all three person use cases process expected failures exclusively with `.onSuccess` and `.onFailure`. They use neither `try/catch/finally`, `getOrThrow()`, nor `resultOf()`. Cancellation propagates; `ensureActive()` prevents late side effects, while `cancelAndJoin()` waits for the previous collector during Retry. Completion handlers only reset `isSaving` and `isEditingImages`; they perform no cleanup.

Persistence precedes image commit. Result failures from subsequent file cleanup are logged without turning a successful deletion into a failure. Koin creates one ImageEdit session per detail ViewModel and shares it between the delegate and both Save use cases. `PersonDependencyInjectionTest` verifies this contract.

Deliberate limitation: ordinary exceptions unexpectedly thrown by repository, commit, or cleanup implementations are not additionally caught or converted to Results in these use cases. Unexpected cleanup throws are not treated as best-effort Results. ViewModel error reporting, loading-state recovery, and retrying Cancel are then not generally guaranteed; Save/image flags still reset on job completion. Existing technical safeguards in the repository, database initialization, and Shared remain intact. Production Shared components return expected file failures through Results or handle session cleanup internally.

Shared's existing Room infrastructure remains supplied infrastructure here. Chapter 5 exposes persistence and introduces DTO, read model, list projection, form model, and extended exception handling. A4 continues to use `Person` for input and domain state; `PersonInputMapping.kt` contains input normalization. `IPersonRepository` already resides in `domain.repositories`.

The Save overlay, image progress indicator, picker handlers, Back behavior, and Snackbar clearance above keyboard and FAB form the common UI baseline with A5. Delete confirmation receives one full name; tests use the real StringProvider with English and German resources.

## Suggested learning path

1. **Select and display:** Open the form, select a gallery image, and trace URI → internal path → state → preview.
2. **Take a photo:** Compare target creation, `TakePicture` result, and file confirmation with the gallery workflow. Also cancel a capture.
3. **Compare Save and Cancel:** Replace an existing image, then cancel; repeat and save. Explain when old and new files may be removed.
4. **Introduce use cases:** Compare A3_04's repository call in the ViewModel with A4_01's call to a Domain use case. Assign UI state, operation coordination, and data access to ViewModel, use case, and repository.
5. **Inspect failure paths:** Read `PersonViewModelImageTest` and `PersonUseCasesTest` for failed copies, replacement cleanup, failed saves, and person deletion with an image.
6. **Connect to A3_04:** Follow confirmed swipe-to-delete into `PersonUcDelete` and explain the additional file-cleanup step.

As a learning outcome, draw both acquisition paths and annotate original/replacement ownership during Save and Cancel.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A4_01_ImagePicker), including image components, file storage, and FileProvider configuration in `Shared`.
