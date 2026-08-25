package de.rogallab.mobile.shared.domain.io

/**
 * File format used when a drawable resource is written to private app storage.
 */
enum class ImageFileFormat(
   val extension: String,
) {
   Jpeg(".jpg"),
   Png(".png"),
   Webp(".webp"),
}
