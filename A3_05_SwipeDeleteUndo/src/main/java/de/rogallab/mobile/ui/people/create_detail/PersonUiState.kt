package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person = Person(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val loadFailure: PersonLoadFailure? = null,
)

sealed interface PersonLoadFailure {
   val message: String

   data class NotFound(override val message: String) : PersonLoadFailure
   data class Failed(override val message: String) : PersonLoadFailure
}
