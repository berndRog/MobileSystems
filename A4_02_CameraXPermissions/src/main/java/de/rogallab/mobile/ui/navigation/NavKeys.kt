package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data object CameraKey : NavKey

// videoUri == null: the viewer starts with the picker.
// videoUri != null: the viewer plays this video immediately (for example the last recording).
@Serializable
data class VideoViewerKey(val videoUri: String? = null) : NavKey