package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

data class PeopleUiState(
   val people: List<Person> = emptyList(),
   val isLoading: Boolean = false,
   val restoredPersonId: String? = null,
)
