package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleIntent {
   data object Create : PeopleIntent
   data class Detail(val personId: String) : PeopleIntent

   // Removes the person only from the visible list and starts the Undo window.
   data class Remove(val person: Person) : PeopleIntent

   // Restores a visually removed person while the Undo action is still valid.
   data class UndoRemove(val personId: String) : PeopleIntent

   // Persists the deletion only after the Undo action was not selected.
   data class CommitRemove(val personId: String) : PeopleIntent
}
