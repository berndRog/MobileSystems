package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person = Person(),
   // isNew == true -> Input else -> Detail
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isWriting: Boolean = false,
)