package de.rogallab.mobile.ui.cars.list

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.common.UiText

sealed interface CarsEvent {
   data object NavigateToCreate : CarsEvent
   data class NavigateToDetails(val carId: String) : CarsEvent
   data class RequestRemove(val car: Car, val originalIndex: Int, ) : CarsEvent
   data class ShowSnackbar(val message: UiText) : CarsEvent
}
