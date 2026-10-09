package de.rogallab.mobile.ui.cars.list

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person

data class CarsUiState(
   val cars: List<Car> = emptyList(),
   val people: List<Person> = emptyList(),
   val isLoading: Boolean = false,
   val carsLoadError: String? = null,
   val peopleLoadError: String? = null,

) {
   val loadError: String? get() = carsLoadError ?: peopleLoadError
}
