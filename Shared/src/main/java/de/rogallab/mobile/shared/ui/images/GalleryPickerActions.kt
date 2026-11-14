package de.rogallab.mobile.shared.ui.images

import androidx.compose.runtime.Stable

// Actions exposed by GalleryPickerHandler to the calling adapter.
@Stable
data class GalleryPickerActions(
   val selectFromGallery: () -> Unit
)
