package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiText

// One-time actions handled by PeopleNavigation or the feature coordinator.
sealed interface PersonEvent {
   data object NavigateBack : PersonEvent

   data class RequestSave(
      val person: Person,
      val isNew: Boolean,
   ) : PersonEvent

   data class ShowSnackbar(
      val message: UiText,
   ) : PersonEvent
}
