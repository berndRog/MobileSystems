package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleCoordinatorEvent {
   // Undo or a failed final DELETE restores the visually removed row.
   data class RestorePerson(
      val person: Person,
      val originalIndex: Int
   ) : PeopleCoordinatorEvent
}