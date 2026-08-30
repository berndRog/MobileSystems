package de.rogallab.mobile.ui.cars.list

import de.rogallab.mobile.domain.entities.Car

sealed interface CarsIntent {
   data object Create : CarsIntent
   data class Open(val carId: String) : CarsIntent
   data class Remove(val car: Car, val originalIndex: Int) : CarsIntent
   data class Restore(val car: Car, val originalIndex: Int) : CarsIntent
   data object Restored : CarsIntent
}
