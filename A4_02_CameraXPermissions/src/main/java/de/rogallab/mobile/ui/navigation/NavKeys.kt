package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data object CameraRoute : NavKey

// videoUri == null: the viewer starts with the picker.
// videoUri != null: the viewer plays this video immediately (for example the last recording).
@Serializable
data class VideoViewerRoute(val videoUri: String? = null) : NavKey