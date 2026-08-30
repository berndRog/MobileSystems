package de.rogallab.mobile.ui.tdrives.list

sealed interface TDrivesEffect {
   data class ShowMessage(val message: String) : TDrivesEffect
   data class ShowError(val message: String) : TDrivesEffect
   data class ConfirmRemove(
      val message: String,
      val actionLabel: String,
      val tDriveId: String,
   ) : TDrivesEffect
   data class NavigateTo(val tDriveId: String?) : TDrivesEffect
}
