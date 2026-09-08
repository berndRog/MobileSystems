package de.rogallab.mobile.testing

import android.net.Uri
import de.rogallab.mobile.shared.domain.io.CameraImageFile
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat

class FakeImageFileStorage : IImageFileStorage {
   val copiedUris = mutableListOf<Uri>()
   val deletedPaths = mutableListOf<String?>()

   var copyResult: Result<String> =
      Result.success("/images/copied.jpg")

   override suspend fun copyImageToAppStorage(
      sourceUri: Uri,
   ): Result<String> {
      copiedUris += sourceUri
      return copyResult
   }

   override suspend fun createCameraImageFile(): Result<CameraImageFile> =
      Result.failure(UnsupportedOperationException("not needed by this test"))

   override suspend fun confirmCameraImageFile(
      imagePath: String,
   ): Result<String> =
      Result.success(imagePath)

   override suspend fun saveDrawableToAppStorage(
      drawableResId: Int,
      fileName: String,
      format: ImageFileFormat,
      quality: Int,
   ): Result<String> =
      Result.success("/images/$fileName")

   override suspend fun deleteImageFromAppStorage(
      imagePath: String?,
   ): Result<Unit> {
      deletedPaths += imagePath
      return Result.success(Unit)
   }
}
