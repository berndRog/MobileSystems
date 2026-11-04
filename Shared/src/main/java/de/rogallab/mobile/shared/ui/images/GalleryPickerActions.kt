package de.rogallab.mobile.shared.ui.images

import androidx.compose.runtime.Stable

/** Exposes the gallery action without exposing the Activity Result launcher. */
@Stable
data class GalleryPickerActions(
   val selectFromGallery: () -> Unit
)
