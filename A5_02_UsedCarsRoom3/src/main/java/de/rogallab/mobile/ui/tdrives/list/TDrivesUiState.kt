package de.rogallab.mobile.ui.tdrives.list

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.TDrive

data class TDrivesUiState(
   val tDrives: List<TDrive> = emptyList(),
   val people: List<Person> = emptyList(),
   val cars: List<Car> = emptyList(),
   val isLoading: Boolean = false,
   val drivesLoadError: String? = null,
   val peopleLoadError: String? = null,
   val carsLoadError: String? = null,

) {
   val loadError: String? get() = drivesLoadError ?: peopleLoadError ?: carsLoadError
}
