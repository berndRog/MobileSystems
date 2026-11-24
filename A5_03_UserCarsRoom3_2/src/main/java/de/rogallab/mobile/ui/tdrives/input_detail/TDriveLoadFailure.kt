package de.rogallab.mobile.ui.tdrives.input_detail

sealed interface TDriveLoadFailure {
   val message: String

   data class NotFound(override val message: String) : TDriveLoadFailure
   data class Failed(override val message: String) : TDriveLoadFailure
}
