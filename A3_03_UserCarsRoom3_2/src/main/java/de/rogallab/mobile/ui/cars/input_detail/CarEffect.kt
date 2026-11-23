package de.rogallab.mobile.ui.cars.input_detail

import de.rogallab.mobile.ui.people.create_detail.BackReason

sealed interface CarEffect {
   data class ShowMessage(val message: String) : CarEffect
   data class ShowError(val message: String) : CarEffect
   data class NavigateBack(val reason: BackReason) : CarEffect
}
