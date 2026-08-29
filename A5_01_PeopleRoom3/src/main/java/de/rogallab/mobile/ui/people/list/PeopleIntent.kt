package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleIntent {
   data object Create : PeopleIntent

   data class Open(
      val personId: String,
   ) : PeopleIntent

   // User action: remove only from the local list and request an Undo window.
   data class Remove(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleIntent

   // Coordinator command after Undo or after a failed final Room DELETE.
   data class Restore(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleIntent

   // The list has completed scrolling to the restored item.
   data object Restored : PeopleIntent
}
