package de.rogallab.mobile.ui.people.input_detail

import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person? = null,
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
)
