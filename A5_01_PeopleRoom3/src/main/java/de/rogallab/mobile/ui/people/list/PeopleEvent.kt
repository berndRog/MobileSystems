package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiText

// One-shot actions which have to be handled outside the list ViewModel.
sealed interface PeopleEvent {
   data object NavigateToCreate : PeopleEvent

   data class NavigateToDetails(
      val personId: String,
   ) : PeopleEvent

   data class RequestRemove(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleEvent

   // Forwards a user-visible list message to the shared Snackbar coordinator.
   data class ShowSnackbar(
      val message: UiText,
   ) : PeopleEvent
}
