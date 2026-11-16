package de.rogallab.mobile.ui.camerax

// Events from the UI to the ViewModel (user actions).
sealed interface CameraIntent {
   data object TakePhoto : CameraIntent
   data class ToggleRecording(val audioGranted: Boolean) : CameraIntent
   data object PlayVideo : CameraIntent
   data object ClosePlayer : CameraIntent
}