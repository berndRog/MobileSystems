package de.rogallab.mobile.ui.cars.input_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person

data class CarUiState(
   val car: Car? = null,
   val registrationYearInput: String = "",
   val mileageInput: String = "",
   val priceInput: String = "",
   val people: List<Person> = emptyList(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
)
