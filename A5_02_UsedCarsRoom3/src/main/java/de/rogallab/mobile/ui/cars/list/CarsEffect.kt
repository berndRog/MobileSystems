package de.rogallab.mobile.ui.cars.list

sealed interface CarsEffect {
   data class ShowMessage(val message: String) : CarsEffect
   data class ShowError(val message: String) : CarsEffect
   data class ConfirmRemove(
      val message: String,
      val actionLabel: String,
      val carId: String,
   ) : CarsEffect
   data class NavigateTo(val carId: String?) : CarsEffect
}
