package de.rogallab.mobile.domain.usecases

import android.net.Uri
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.IImageMediaStore
import de.rogallab.mobile.shared.domain.utilities.Alog

/**
 * Publishes a captured private photo in the shared gallery.
 *
 * Verifies the private file, exports it to the MediaStore and always removes the
 * private copy afterwards, whether the export succeeded or not.
 */
class PhotoUcSave(
   private val _imageFileStorage: IImageFileStorage,
   private val _imageMediaStore: IImageMediaStore,
) {
   suspend operator fun invoke(
      imagePath: String,
      groupName: String,
      fileName: String,
   ): Result<Uri> {
      try {
         // A cancelled or failed capture may leave an empty file: verify first.
         val confirmedPath = _imageFileStorage.confirmCameraImageFile(imagePath)
            .getOrElse { return Result.failure(it) }

         return _imageMediaStore.exportImageToMediaStore(
            imagePath = confirmedPath,
            groupName = groupName,
            fileName = fileName,
         )
      }
      finally {
         // The private file is no longer needed.
         _imageFileStorage.deleteImageFromAppStorage(imagePath)
            .onFailure { Alog.e(TAG, "delete private image failed: ${it.message}") }
      }
   }

   companion object {
      private const val TAG = "<-PhotoUcSave"
   }
}