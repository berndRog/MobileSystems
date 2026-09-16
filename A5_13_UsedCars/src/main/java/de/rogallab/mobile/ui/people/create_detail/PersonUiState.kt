package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person = Person(),
   val cars: List<Car> = emptyList(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isCarsLoading: Boolean = false,
)
