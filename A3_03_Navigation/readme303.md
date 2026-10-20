# A3_03_Navigation

## Purpose and prerequisites

This module connects the person list and create/detail form using **Navigation 3**. In A3_02, effects originating from the ViewModel display messages or errors in a Snackbar. A3_03 extends this mechanism: **the ViewModel emits a navigation effect, and the navigation layer changes the back stack.** Screens no longer need to be selected manually in `MainActivity`.

Students should understand the Material 3 UI from A3_01 and the State–Intent–Effect flow, adapters, and Snackbar handling from A3_02.

## Learning objectives

After studying the module, students should be able to:

- Trace a user action through an intent and ViewModel effect to a navigation callback and back-stack change.
- Describe destinations using serializable `NavKey` types and pass a person ID to a shared create/detail destination.
- Explain the roles of `rememberNavBackStack`, `NavDisplay`, and `entryProvider`.
- Distinguish navigation effects from message/error effects and route each to the responsible UI component.
- Explain how entry decorators support saveable Compose state and ViewModels per navigation entry.
- Keep Snackbar output available across screen changes and distinguish Save and Cancel through their back-navigation reasons and animations.

## Concrete navigation flow

**UI action → Intent → ViewModel → navigation effect → adapter callback → `AppNavigation` → back stack**

| Trigger | Effect emitted by the ViewModel | Result in `AppNavigation` |
| --- | --- | --- |
| Create FAB | `PeopleEffect.NavigateTo(null)` | Add `PersonKey(null)`; open an empty form |
| Tap a person's `Card` | `PeopleEffect.NavigateTo(personId)` | Add `PersonKey(personId)`; load the existing person |
| Successful Save | `ShowMessage`, then `NavigateBack(BackReason.Save)` | Start Snackbar output and remove the form entry |
| Cancel | `NavigateBack(BackReason.Cancel)` | Remove the form entry without saving |

A failed save emits `ShowError` and keeps the form open. Navigation follows successful persistence. The form's toolbar arrow triggers Save when the form is available; during a load failure it triggers Cancel. System Back is handled directly by `NavDisplay.onBack`, which removes the current entry with the Cancel animation.

## Didactic approach

Keep the person example and effect infrastructure familiar, then introduce the back stack as the new concept. `PeopleKey` represents the list and remains the root. `PersonKey` carries an optional ID: `null` selects create mode; an existing ID selects detail mode. Koin supplies the corresponding ViewModel with that ID.

`AppNavigation` starts with `rememberNavBackStack(PeopleKey)`. Adding a key opens a destination; removing the last key returns to the previous destination. The removal helper preserves the root. Adapters translate concrete effects into callbacks, while `AppNavigation` owns the back-stack operations. ViewModels remain independent of Navigation 3 APIs.

Message effects follow a separate path to `SnackbarController`. One `SnackbarHostState`, host, and controller live above `NavDisplay`. Their scope survives a destination change, allowing a save confirmation to remain visible after returning to the list. State continues to be collected lifecycle-aware; load failures remain in state. A missing person offers Back, while a repository load failure offers Retry.

Save uses a horizontal return animation; Cancel fades the previous screen in while the form exits downward. A separate scale/fade transition supports predictive Back.

## Suggested learning path

1. **Trace Create and Detail:** Follow both actions from `PeopleViewModel` through `PeopleAdapter` to `AppNavigation` and compare their `PersonKey` values.
2. **Inspect the stack:** Open a person, return, and compare the logged add/pop operations. Explain why `PeopleKey` remains.
3. **Compare outcomes:** Save successfully, cancel, and examine the failed-save path in `PersonBackEffectTest`. Relate effects, Snackbar output, and navigation.
4. **Explain ownership:** Sketch which component owns application state, effect handling, the back stack, and Snackbar output. Consult `NavigationEffectTest` for the emitted destination IDs.

A3_04 builds on this working navigation flow by adding swipe-to-detail and swipe-to-delete with Snackbar confirmation. Undo follows in A3_05.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_03_Navigation).
