package de.rogallab.mobile.ui.tdrives.input_detail

import de.rogallab.mobile.ui.people.create_detail.BackReason

sealed interface TDriveEffect {
   data class ShowMessage(val message: String) : TDriveEffect
   data class ShowError(val message: String) : TDriveEffect
   data class NavigateBack(val reason: BackReason) : TDriveEffect
}
