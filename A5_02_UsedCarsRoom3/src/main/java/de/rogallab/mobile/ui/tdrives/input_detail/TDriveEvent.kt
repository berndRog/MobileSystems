package de.rogallab.mobile.ui.tdrives.input_detail

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.ui.common.UiText

sealed interface TDriveEvent {
   data object NavigateBack : TDriveEvent
   data class RequestSave(
      val tDrive: TDrive,
      val isNew: Boolean,
   ) : TDriveEvent
   data class ShowSnackbar(val message: UiText) : TDriveEvent
}
