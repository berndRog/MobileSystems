package de.rogallab.mobile.ui.cars.input_detail

sealed interface CarLoadFailure {
   val message: String

   data class NotFound(override val message: String) : CarLoadFailure
   data class Failed(override val message: String) : CarLoadFailure
}
