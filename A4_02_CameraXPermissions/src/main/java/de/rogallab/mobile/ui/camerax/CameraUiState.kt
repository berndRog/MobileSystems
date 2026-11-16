package de.rogallab.mobile.ui.camerax

import androidx.camera.core.SurfaceRequest

// Persistent UI state: everything the screen needs to render itself.
data class CameraUiState(
   val surfaceRequest: SurfaceRequest? = null,   // image source for the viewfinder
   val isRecording: Boolean = false,
)