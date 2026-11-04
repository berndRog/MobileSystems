package de.rogallab.mobile.shared.ui.images

import androidx.compose.runtime.Stable

//Actions exposed by CameraPickerHandler to the calling adapter.
@Stable
data class CameraPickerActions(
   val isBusy: Boolean,
   val takePhoto: () -> Unit,
)