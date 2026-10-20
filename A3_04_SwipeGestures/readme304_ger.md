# A3_05_SwipeDeleteUndo

## Purpose and prerequisites

This optional extension builds on **A3_04_SwipeGestures**. A3_04 asks for confirmation before deleting. A3_05 changes the interaction: **a swipe immediately hides the person, and a Snackbar offers Undo. Repository deletion is postponed until the Snackbar ends without Undo.**

The list (`PeopleAdapter`/`PeopleScreen`), form (`PersonAdapter`/`PersonScreen`), Navigation 3, and reusable `SwipeCard` remain in use. Students should understand the gesture, Intent, ViewModel, Effect, and Snackbar paths from the preceding modules.

## Learning objectives

After studying the module, students should be able to:

- Distinguish immediate visual removal from the later repository deletion.
- Trace `Remove`, `ShowUndo`, `UndoRemove`, and `CommitRemove` through their respective Intent and Effect paths.
- Explain why Undo requires no compensating repository write in this implementation.
- Use `IVisualRemoval<Person>` and `VisualRemovalDelegate<T>` to separate temporary removal state from persistence and Snackbar output.
- Restore the visible entry after a failed repository deletion and report the failure through `ShowError`.
- Explain how stable keys, item animation, and scrolling make a restored entry visible to the user.

## Concrete Swipe-to-Delete/Undo flow

`EndToStart` invokes the delete callback. `PeopleAdapter` finds the person and sends `PeopleIntent.Remove(person)`. The ViewModel hides the person through `IVisualRemoval`, publishes the visible list, and emits `PeopleEffect.ShowUndo(message, actionLabel, personId)`.

**Swipe → `Remove` intent → visual removal + `ShowUndo` effect → adapter → action Snackbar**

The effect originates from the ViewModel and requests a UI action. `PeopleAdapter` forwards it through `onUndo`; `AppNavigation` calls `SnackbarController.showAction()`. The Snackbar result determines the next intent:

| Result | Intent returned to the ViewModel | Outcome |
| --- | --- | --- |
| User selects Undo | `UndoRemove(personId)` | Restore visibility; no repository deletion |
| Snackbar closes or times out without Undo | `CommitRemove(personId)` | Call `repository.remove(person)` |
| Repository deletion fails | — | Restore visibility and emit `ShowError` |

`StartToEnd` continues to open the existing person's detail form. Creating a new person remains a FAB action.

## Didactic approach

Use the contrast with A3_04 to introduce two distinct states: the latest repository data and the currently visible list. `VisualRemovalDelegate<T>` in `Shared` keeps source items, hidden IDs, and pending removals. `visibleItems()` filters hidden entries from the source list. Repository emissions update the source without prematurely revealing pending removals.

Undo clears the pending/hidden state; no persistent deletion has happened yet. After a successful commit, the delegate keeps the ID hidden until the observed source confirms its removal. This handles either order of repository emissions and operation completion. The delegate itself has no repository, Compose, coroutine, or Effect dependency; the ViewModel coordinates these responsibilities.

`PeopleViewModel` and the Snackbar infrastructure are obtained above `NavDisplay` in `AppNavigation`, allowing pending Snackbar callbacks to address the same ViewModel across destination changes. State is collected lifecycle-aware in the adapter.

After Undo or a failed commit, `restoredPersonId` identifies the restored entry. `PeopleScreen` uses `ScrollToItemIfNotVisible` to bring it into view when needed. `RestoreHandled` then clears the target. Stable person IDs and `animateItem()` support removal, reinsertion, and movement.

## Suggested learning path and optional depth

**Core, if teaching time is limited:**

1. Compare A3_04's confirmation with A3_05's immediate visual removal.
2. Trace one swipe through `Remove` and `ShowUndo`; compare Undo with letting the Snackbar expire.
3. Explain when the repository is changed and why Undo needs no database insert.

**Extension, if time permits:**

- Inspect `VisualRemovalDelegate` and the ordering of source updates and commit completion.
- Study `PeopleViewModelSwipeTest`: visual removal without repository access, Undo, delayed commit, failed-commit restoration, and duplicate removal requests.
- Study `PeopleScreenRestoreTest`: restored entries at the beginning/end of the list become visible and are acknowledged.

A suitable learning outcome is a sequence sketch showing the visible list and repository contents at each step. This chapter can be taught as a demonstration first, with delegate internals and scroll handling reserved for later study.

Source basis: [MobileSystems, revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_05_SwipeDeleteUndo), including removal, scrolling, and Snackbar infrastructure in `Shared`.
