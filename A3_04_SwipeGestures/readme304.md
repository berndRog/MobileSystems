# A3_04_SwipeGestures

## Purpose and prerequisites

This module extends **A3_03_Navigation** with horizontal swipe gestures on the person list. `PeopleScreen` wraps each `PersonCard` in the reusable **`SwipeCard` from `Shared`**. One direction opens the existing person in the form (`PersonAdapter`/`PersonScreen`); the other requests deletion through a confirmation Snackbar.

Students should understand Material 3 components, `LazyColumn`, state hoisting, and the Intent–ViewModel–Effect–adapter flow, including Navigation 3 and Snackbar output.

## Learning objectives

After studying the module, students should be able to:

- Use Material 3 `SwipeToDismissBox` through a reusable `SwipeCard` and supply the concrete `Card` content as a composable lambda.
- Map `StartToEnd` to Detail and `EndToStart` to a delete request.
- Separate gesture recognition, UI callbacks, intents, ViewModel effects, and repository operations.
- Explain why requesting deletion and confirming deletion are separate steps.
- Provide feedback during a swipe using direction-dependent colors and icons.
- Explain swipe-state reset, current callbacks across recompositions, stable list keys, and `Modifier.animateItem()`.

## Concrete gesture and effect flow

| Gesture | Intent sent through `PeopleAdapter` | Effect emitted by `PeopleViewModel` | UI result |
| --- | --- | --- | --- |
| `StartToEnd` | `Detail(personId)` | `NavigateTo(personId)` | `AppNavigation` opens `PersonKey(personId)` |
| `EndToStart` | `RequestRemove(personId)` | `ConfirmRemove(message, actionLabel, personId)` | An action Snackbar asks for confirmation |

For deletion, the full sequence is:

**Swipe → `RequestRemove` intent → `ConfirmRemove` effect → Snackbar action → `ConfirmRemove` intent → repository deletion → updated list**

`PeopleAdapter` forwards the confirmation effect to `onConfirmRemove`. In `AppNavigation`, `SnackbarController.showAction()` displays the request. Only selecting its action sends `PeopleIntent.ConfirmRemove(personId)` back to the ViewModel. Dismissing the Snackbar or letting it time out leaves the person unchanged.

After successful deletion, the ViewModel emits `ShowMessage`; a failure emits `ShowError`. The repository's observed list supplies the new UI state. The swipe alone does not delete the person or remove them optimistically from the list.

## Didactic approach

Introduce gestures as another input path into the existing architecture. Navigation and Snackbar handling from A3_03 remain in use. `PeopleScreen` receives values and callbacks; it has no repository or navigation dependency. Creating a new person remains a separate FAB action.

`SwipeCard` encapsulates gesture mechanics without knowing `Person`. Its `content` lambda supplies `PersonCard`; localized content descriptions come from the caller. This makes the distinction between reusable interaction and application-specific content concrete.

The background follows `dismissDirection`: Detail uses `primaryContainer` with an Edit icon, deletion uses `errorContainer` with a Delete icon. Students can distinguish feedback during movement from the action after a completed swipe.

Before invoking the callback, `SwipeCard` resets its state with `snapTo(Settled)`. `remember` keeps the dismiss handler stable, while `rememberUpdatedState` supplies current callbacks. In `LazyColumn`, person IDs identify entries across list changes; `animateItem()` animates removal and movement after the repository updates the list.

## Suggested learning path

1. **Observe both directions:** Compare the revealed colors/icons and the resulting Detail screen or confirmation Snackbar.
2. **Trace Detail:** Follow the callback from `SwipeCard` through `PeopleScreen`, `PeopleAdapter`, and `PeopleViewModel` to the navigation effect.
3. **Compare confirmation outcomes:** Request deletion, dismiss the Snackbar, then request again and confirm. Explain exactly when repository deletion occurs.
4. **Inspect implementation and tests:** Relate state reset and callbacks to `SwipeCardTest`. Read `PeopleViewModelSwipeTest` for confirmation without deletion, successful deletion, and a failed deletion that keeps the person.

A suitable learning outcome is a sequence sketch separating delete request, confirmation effect, confirmation intent, repository result, and list update.

## Position in the learning sequence

**A3_01:** Material UI and reusable input → **A3_02:** message/error effects → **A3_03:** navigation effects and back stack → **A3_04:** gestures and confirmed deletion.

A3_04 confirms before deleting. A3_05 introduces visual removal with Undo, delayed repository commit, and restoration after failure.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_04_SwipeGestures), including `SwipeCard` and Snackbar infrastructure in `Shared`.
