package de.rogallab.mobile.shared.ui.images

import androidx.compose.runtime.Stable

// Exposes the video selection without exposing the Activity Result launcher.
@Stable
data class VideoPickerActions(
   val selectVideo: () -> Unit,
)