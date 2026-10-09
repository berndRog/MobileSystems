package de.rogallab.mobile.ui.cars.input_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person

data class CarUiState(
   val car: Car? = null,
   val mileageInput: String = "",
   val priceInput: String = "",
   val people: List<Person> = emptyList(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val carLoadError: String? = null,
   val peopleLoadError: String? = null,
   val notFound: Boolean = false,

) {
   val loadError: String? get() = carLoadError ?: peopleLoadError
}
