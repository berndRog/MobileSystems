# A3_02_EffectHandling

## Purpose and prerequisites

This module extends **A3_01_Material** with **effects originating from the ViewModel**. An effect tells the UI which action to perform in response to a result or decision in the ViewModel. In A3_02, effects display informational or error messages in a Snackbar. A3_03 later uses effects to request navigation between screens.

For example, after successfully saving a person, `PersonViewModel` emits `ShowMessage`. If saving fails, it emits `ShowError`. The adapter receives the effect and forwards its text to the Snackbar output. The ViewModel initiates the action; the UI performs it.

Students should understand Compose, state hoisting, the adapter–screen separation, intents, and basic `StateFlow` usage from A3_01.

## Learning objectives

After studying the module, students should be able to:

- Distinguish `UiState`, which describes the current screen state, from effects through which the ViewModel requests a UI action: `ShowMessage` displays an informational Snackbar, and `ShowError` displays an error Snackbar. Explain how navigation effects extend this mechanism in A3_03.
- Model feature-specific effects with sealed interfaces (`PersonEffect`, `PeopleEffect`).
- Explain how `EffectDelegate<E>` exposes a buffered `Channel` through `receiveAsFlow()`, and how Kotlin interface delegation with `by` avoids repeating infrastructure in each ViewModel.
- Separate generic effect collection from feature-specific handling in an adapter.
- Connect message callbacks to Material 3 Snackbar output through `SnackbarController` and `SnackbarHostState`.
- Keep load failures visible in state, offer an explicit retry, and test state changes separately from emitted effects.

## Didactic approach

The module preserves the architecture from A3_01 and adds a second output path from the ViewModel:

**User action → Intent → ViewModel → Effect → adapter → Snackbar**

Intents carry user actions into the ViewModel. Effects carry resulting requests out of the ViewModel to the UI. For a successful save, the concrete path is: `PersonIntent.Save` → repository save → `PersonEffect.ShowMessage` → `onMessage` → `SnackbarController.showMessage()`. On a save failure, the path uses `ShowError`, `onError`, and `showError()` instead. In A3_03, `NavigateTo` and `NavigateBack` extend this output path to navigation; the navigation layer updates the back stack.

| Information | Example | Processing |
| --- | --- | --- |
| Persistent state | Person data, `isLoading`, `isSaving`, `loadFailure` | `StateFlow` → adapter → screen |
| One-time effect | Save confirmation or action error | Effect flow → adapter → message callback → Snackbar |

A load failure remains relevant until another load attempt succeeds. It therefore belongs in state and is displayed with Retry. A save confirmation or save/delete failure concerns an individual action and is emitted as an effect. Form validation feedback remains in place; the final validation before saving can additionally emit `ShowError`.

`EffectHandler` collects the effect flow in `LaunchedEffect`. The adapter interprets the concrete effect and calls `onMessage` or `onError`. `SnackbarController` encapsulates the coroutine needed by the suspending `showSnackbar()` call. Messages are short; errors use a longer duration and offer dismissal. ViewModels resolve resource texts through `IStringProvider`; effects carry ready-to-display strings.

Use one UI consumer per feature effect flow: the underlying channel distributes events between consumers rather than broadcasting every event to all of them.

## Suggested learning path

1. **Compare with A3_01:** Trace the list's delete action and identify where logging is supplemented by `ShowMessage` or `ShowError`.
2. **Follow both paths:** Trace a state update and a message effect from the ViewModel to their respective UI consumers.
3. **Explore saving:** In `ui/MainActivity.kt`, select `PersonAdapter` instead of `PeopleAdapter`, retaining the Snackbar parameters and callbacks. Use `null` as the person ID for create mode or an existing ID for detail mode. Compare invalid input and successful saving.
4. **Study failures:** Read `PersonViewModelEffectTest` and `PeopleViewModelEffectTest`. Explain the tested differences between a persistent load failure, Retry, a save error effect, and blocked actions during saving.

As a learning outcome, annotate both output paths and justify the classification of one load failure and one action result.

## Position in the learning sequence

A3_01 establishes Material UI, validation, mapping, and state. **A3_02 adds effect handling independently of navigation.** `MainActivity` currently selects the list; create/detail screens are selected manually. Create and Detail intents do not yet navigate, and the form's toolbar arrow triggers Save. Cancel clears first and last name.

A3_03 connects the screens through Navigation 3 and navigation effects. A3_04 adds swipe gestures with Snackbar confirmation before deletion; Undo follows in A3_05.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_02_EffectHandling), including the effect infrastructure in `Shared`.
