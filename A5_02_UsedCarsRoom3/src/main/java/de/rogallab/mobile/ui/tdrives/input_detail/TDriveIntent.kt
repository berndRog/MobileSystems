package de.rogallab.mobile.ui.tdrives.input_detail

sealed interface TDriveIntent {
   data class PersonChanged(val personId: String?) : TDriveIntent
   data class CarChanged(val carId: String?) : TDriveIntent
   data class StartChanged(val value: String) : TDriveIntent
   data class NotesChanged(val value: String) : TDriveIntent
   data class CompletedChanged(val value: Boolean) : TDriveIntent
   data object Save : TDriveIntent
   data object Cancel : TDriveIntent
}
