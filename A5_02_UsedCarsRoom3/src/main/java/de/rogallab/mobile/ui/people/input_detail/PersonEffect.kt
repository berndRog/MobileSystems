package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiText

sealed interface PersonEffect {
   data object NavigateBack : PersonEffect
   data class RequestSave(
      val person: Person,
      val isNew: Boolean,
   ) : PersonEffect
   data class ShowSnackbar(val message: UiText) : PersonEffect
}
