package de.rogallab.mobile.ui.tdrives.list

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.ui.common.UiText

sealed interface TDrivesEvent {
   data object NavigateToCreate : TDrivesEvent
   data class NavigateToDetails(val testDriveId: String) : TDrivesEvent
   data class RequestRemove(
      val tDrive: TDrive,
      val originalIndex: Int,
   ) : TDrivesEvent
   data class ShowSnackbar(val message: UiText) : TDrivesEvent
}
