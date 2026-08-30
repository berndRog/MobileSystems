package de.rogallab.mobile.ui.cars.input_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.common.UiText

sealed interface CarEvent {
   data object NavigateBack : CarEvent
   data class RequestSave(
      val car: Car,
      val isNew: Boolean,
   ) : CarEvent
   data class ShowSnackbar(val message: UiText) : CarEvent
}
