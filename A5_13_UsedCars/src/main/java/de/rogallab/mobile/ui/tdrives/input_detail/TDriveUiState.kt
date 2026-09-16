package de.rogallab.mobile.ui.tdrives.input_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.TDrive

data class TDriveUiState(
   val tDrive: TDrive? = null,
   val startInput: String = "",
   val people: List<Person> = emptyList(),
   val cars: List<Car> = emptyList(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
)
