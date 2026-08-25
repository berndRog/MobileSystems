package de.rogallab.mobile.shared.domain.io

import android.net.Uri

/**
 * Provides image operations for Android's shared MediaStore.
 *
 * Images exported through this interface are visible to gallery applications
 * and remain independent from the private source image.
 */
interface IImageMediaStore {

    // Copies a private image file into the public Pictures collection.
    //
    // imagePath: Absolute path of the private source image.
    // groupName: Subdirectory below Pictures, for example "PeopleRoom3".
    // fileName: Optional public name. The source extension is preserved.
    // result<Uri> of the newly exported MediaStore image.
   suspend fun exportImageToMediaStore(
      imagePath: String,
      groupName: String,
      fileName: String? = null,
   ): Result<Uri>


   // Deletes one concrete MediaStore image URI owned by the app.
   // A missing item is treated as already deleted.
   suspend fun deleteImageFromMediaStore(imageUri: Uri): Result<Unit>
}
