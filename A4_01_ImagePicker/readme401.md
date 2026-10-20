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
| Gallery | `GalleryPickerHandlerSingle` | Photo Picker → content URI → `GalleryImageSelected` intent → copy to app storage in the ViewModel |
| Camera | `CameraPickerHandler` | Prepare target file/URI → camera → confirm file → `CameraImageTaken` intent with internal path |

**Gallery:** `PickVisualMedia` selects one image. Cancelling the picker produces no selection event. The ViewModel copies the selected image through `IImageFileStorage.copyImageToAppStorage()`. This module stores an app-owned image path instead of persisting the picker URI.

**Camera:** `TakePicture` needs a writable destination before launch and returns a Boolean result. The handler creates a provisional file and passes its FileProvider URI to the external camera. It remembers the associated internal path, confirms a successful capture, and attempts cleanup on cancellation or failure. The FileProvider is declared in `Shared`.

Both paths then converge: **internal path → `IImageEdit.replace()` → `PersonUiState.person.imagePath` → image display**.

## Didactic approach

Start with the visible actions, then compare their different technical requirements. `PersonScreen` uses `ImageSelection` and receives `onSelectPhoto`, `onTakePhoto`, and `onRemovePhoto`. `PersonAdapter` connects these callbacks to the handlers or intents and observes state lifecycle-aware. `ImageRenderer` uses Coil's `AsyncImage` or a placeholder icon when no image exists.

Separate obtaining a file from managing its edit session. `IImageFileStorage` performs file operations; `ImageEdit` tracks original and currently selected images. A person uses one image, although the delegate accepts lists of paths.

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
| `PersonUcDelete` | `repository.remove(person)` → on success, attempt image-file cleanup |

The ViewModel processes intents, manages UI state, validates the form, selects the matching use case, and translates its result into effects. The use case coordinates the application operation; the repository provides person-data access. Dependencies are supplied through constructor injection. `suspend operator fun invoke(person)` lets the ViewModel call a use-case instance like a function, for example `_personUcUpdate(person)`.

The learning step is to move the combined persistence/image operation out of the ViewModel and make its ordering independently testable. A failed repository write does not commit the image session; a failed person deletion leaves the image intact. Cleanup failures after successful person deletion are logged.

Image-copy failures and camera errors become `ShowError` effects from the ViewModel and are displayed in a Snackbar. Successful saving emits `ShowMessage`, followed by `NavigateBack`. The image path remains state; the request for a message or navigation is an effect.

## Suggested learning path

1. **Select and display:** Open the form, select a gallery image, and trace URI → internal path → state → preview.
2. **Take a photo:** Compare target creation, `TakePicture` result, and file confirmation with the gallery workflow. Also cancel a capture.
3. **Compare Save and Cancel:** Replace an existing image, then cancel; repeat and save. Explain when old and new files may be removed.
4. **Introduce use cases:** Compare A3_04's repository call in the ViewModel with A4_01's call to a Domain use case. Assign UI state, operation coordination, and data access to ViewModel, use case, and repository.
5. **Inspect failure paths:** Read `PersonViewModelImageTest` and `PersonUseCasesTest` for failed copies, replacement cleanup, failed saves, and person deletion with an image.
6. **Connect to A3_04:** Follow confirmed swipe-to-delete into `PersonUcDelete` and explain the additional file-cleanup step.

As a learning outcome, draw both acquisition paths and annotate original/replacement ownership during Save and Cancel.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A4_01_ImagePicker), including image components, file storage, and FileProvider configuration in `Shared`.
