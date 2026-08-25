package de.rogallab.mobile.shared.domain.io

import android.net.Uri

/**
 * Provides image operations for the app's private files directory.
 *
 * The interface intentionally returns absolute file paths. These paths can be
 * stored in Room without retaining access to the original content URI.
 */
interface IImageFileStorage {

   // Copies an image from a readable content URI into private app storage.
   // The URI may originate from the Photo Picker, MediaStore, another
   // ContentProvider or a FileProvider.
   suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String>

   // Creates an empty private JPEG file and its FileProvider URI for TakePicture.
   suspend fun createCameraImageFile(): Result<CameraImageFile>

    // Verifies that a camera application wrote a non-empty private image file.
    // The verified absolute path is returned unchanged.
   suspend fun confirmCameraImageFile(imagePath: String): Result<String>

   // Writes a drawable resource into private app storage.
   suspend fun saveDrawableToAppStorage(
      drawableResId: Int,
      fileName: String,
      format: ImageFileFormat = ImageFileFormat.Png,
      quality: Int = 100,
   ): Result<String>

   // Deletes a private image. A null, blank or missing path is treated as success.
   suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit>
}
