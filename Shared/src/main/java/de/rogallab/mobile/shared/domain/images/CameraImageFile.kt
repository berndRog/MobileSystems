package de.rogallab.mobile.shared.domain.images

import android.net.Uri

/**
 * Result of preparing a private image file for a camera application.
 *
 * @property imagePath Absolute path stored later in an entity or UI state.
 * @property contentUri Temporary FileProvider Uri passed to the camera application.
 */
data class CameraImageFile(
   val imagePath: String,
   val contentUri: Uri,
)
