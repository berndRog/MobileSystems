package de.rogallab.mobile.shared.domain.images

data class ImageChange(
   // null means unchanged; an empty list means the selection was cleared.
   val paths: List<String>? = null,
   val failedImports: Int = 0,
)
