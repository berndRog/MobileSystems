package de.rogallab.mobile.ui.cars.list

sealed interface CarsIntent {
   data object Create : CarsIntent
   data class Detail(val carId: String) : CarsIntent
   data class RequestRemove(val carId: String) : CarsIntent
   data class ConfirmRemove(val carId: String) : CarsIntent
}
