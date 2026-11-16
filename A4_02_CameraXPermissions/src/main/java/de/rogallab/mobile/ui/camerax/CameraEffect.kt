package de.rogallab.mobile.ui.camerax

// One-time events from the ViewModel to the UI. Nothing else uses effects.
sealed interface CameraEffect {
   data class ShowMessage(val message: String) : CameraEffect
   data class ShowError(val message: String) : CameraEffect
}