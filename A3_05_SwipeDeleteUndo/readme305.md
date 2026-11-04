# A3_05_SwipeDeleteUndo

## Purpose and prerequisites

This optional extension builds on **A3_04_SwipeGestures**. A3_04 requests confirmation before deletion. A3_05 changes the interaction: **a swipe immediately hides the person, and a Snackbar offers Undo. Repository deletion is postponed until the Snackbar ends without Undo.**

The list (`PeopleAdapter`/`PeopleScreen`), form (`PersonAdapter`/`PersonScreen`), Navigation 3, and reusable `SwipeCard` remain in use. Students should understand the gesture, Intent, ViewModel, Effect, and Snackbar paths from the preceding projects.

## Learning objectives

After studying the project, students should be able to:

- Distinguish immediate visual removal from the later repository deletion.
- Trace `Remove`, `ShowUndo`, `UndoRemove`, and `CommitRemove` through their Intent and Effect paths.
- Explain why Undo needs no compensating repository write in this implementation.
- Use `IVisualRemoval<Person>` and `VisualRemovalDelegate<T>` to separate temporary removal state from persistence and Snackbar output.
- Restore an item after a failed repository deletion and report the failure through `ShowError`.
- Explain how stable keys, item animation, and scrolling make a restored item visible.

## Concrete swipe-to-delete and Undo flow

`EndToStart` invokes the delete callback. `PeopleAdapter` finds the person and sends `PeopleIntent.Remove(person)`. The ViewModel hides the person through `IVisualRemoval`, publishes the visible list, and emits `PeopleEffect.ShowUndo(message, actionLabel, personId)`.

**Swipe → `Remove` intent → visual removal and `ShowUndo` effect → adapter → action Snackbar**

`PeopleAdapter` forwards the effect through `onUndo`; `AppNavigation` calls `SnackbarController.showAction()`. The Snackbar result determines the next intent:

| Result | Intent returned to the ViewModel | Outcome |
| --- | --- | --- |
| User selects Undo | `UndoRemove(personId)` | Restore visibility; no repository deletion |
| Snackbar closes or times out without Undo | `CommitRemove(personId)` | Call `repository.remove(person)` |
| Repository deletion fails | — | Restore visibility and emit `ShowError` |

`StartToEnd` continues to open the existing person's detail form. Creating a new person remains a FAB action.

## Didactic approach

Compare two states: the latest repository data and the currently visible list. `VisualRemovalDelegate<T>` in `Shared` keeps source items, hidden IDs, and pending removals. `visibleItems()` filters hidden entries from the source list. Repository emissions update the source without prematurely revealing pending removals.

Undo clears the pending and hidden state; no persistent deletion has happened yet. After a successful commit, the delegate keeps the ID hidden until the observed source confirms its removal. This handles either order of repository emissions and operation completion. The delegate itself has no repository, Compose, coroutine, or Effect dependency; the ViewModel coordinates them.

`PeopleViewModel` and the Snackbar infrastructure are obtained above `NavDisplay` in `AppNavigation`, allowing pending Snackbar callbacks to address the same ViewModel across destination changes. The adapter collects state with lifecycle awareness.

After Undo or a failed commit, `restoredPersonId` identifies the restored item. `PeopleScreen` uses `ScrollToItemIfNotVisible` to bring it into view when needed. `RestoreHandled` then clears the target. Stable person IDs and `animateItem()` support removal, reinsertion, and movement.

## Suggested learning path and optional depth

**Core, if teaching time is limited:**

1. Compare A3_04's confirmation with A3_05's immediate visual removal.
2. Trace one swipe through `Remove` and `ShowUndo`; compare Undo with letting the Snackbar expire.
3. Explain when the repository changes and why Undo needs no database insert.

**Extension, if time permits:**

- Inspect `VisualRemovalDelegate` and the ordering of source updates and commit completion.
- Study `PeopleViewModelSwipeTest`: visual removal without repository access, Undo, delayed commit, failed-commit restoration, and duplicate removal requests.
- Study `PeopleScreenRestoreTest`: restored entries at the beginning or end of the list become visible and are acknowledged.

A useful learning outcome is a sequence sketch showing the visible list and repository contents at each step. Delegate internals and scroll handling can be studied after the core interaction.
