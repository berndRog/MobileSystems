package de.rogallab.mobile.ui.tdrives.list

sealed interface TDrivesIntent {
   data object Create : TDrivesIntent
   data class Detail(val tDriveId: String) : TDrivesIntent
   data class RequestRemove(val tDriveId: String) : TDrivesIntent
   data class ConfirmRemove(val tDriveId: String) : TDrivesIntent
}
