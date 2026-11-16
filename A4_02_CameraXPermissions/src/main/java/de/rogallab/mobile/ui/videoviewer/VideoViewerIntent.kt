package de.rogallab.mobile.ui.videoviewer

import android.net.Uri

sealed interface VideoViewerIntent {
   data class VideoSelected(val uri: Uri) : VideoViewerIntent
   data object ClearVideo : VideoViewerIntent
}