package de.rogallab.mobile.data.local.io

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Holds the private image path and the temporary FileProvider URI used by
// the external camera application.
data class CameraImageFile(
   val imagePath: String,
   val contentUri: Uri,
)

// Copies a Photo Picker image into the private app directory.
// Room stores the new local path, so no persistent permission for the source
// URI is required after the copy operation.
suspend fun copyImageToAppStorage(
   context: Context,
   sourceUri: Uri,
): Result<String> = withContext(Dispatchers.IO) {
   runCatching {
      val extension = when (context.contentResolver.getType(sourceUri)) {
         "image/png" -> PNG_EXTENSION
         else -> JPEG_EXTENSION
      }

      val targetFile = createImageFile(
         context = context,
         extension = extension,
      )

      try {
         context.contentResolver
            .openInputStream(sourceUri)
            ?.use { inputStream ->
               targetFile.outputStream().use { outputStream ->
                  inputStream.copyTo(outputStream)
               }
            }
            ?: error("The selected image could not be opened.")

         targetFile.absolutePath
      }
      catch (throwable: Throwable) {
         targetFile.delete()
         throw throwable
      }
   }
}

// Creates an empty JPEG file and a FileProvider URI for the camera app.
fun createCameraImageFile(
   context: Context,
): Result<CameraImageFile> = runCatching {
   val imageFile = createImageFile(
      context = context,
      extension = JPEG_EXTENSION,
   )

   try {
      val contentUri = FileProvider.getUriForFile(
         context,
         "${context.packageName}.fileprovider",
         imageFile,
      )

      CameraImageFile(
         imagePath = imageFile.absolutePath,
         contentUri = contentUri,
      )
   }
   catch (throwable: Throwable) {
      imageFile.delete()
      throw throwable
   }
}

// Deletes a private image file. A missing file counts as already deleted.
fun deleteImageFromAppStorage(
   imagePath: String?,
): Boolean {
   val path = imagePath?.takeUnless(String::isBlank)
      ?: return true

   val file = File(path)
   return !file.exists() || file.delete()
}

private fun createImageFile(
   context: Context,
   extension: String,
): File {
   val directory = File(
      context.filesDir,
      IMAGE_DIRECTORY_NAME,
   ).apply {
      check(exists() || mkdirs()) {
         "The image directory could not be created."
      }
   }

   return File(
      directory,
      "${UUID.randomUUID()}$extension",
   ).apply {
      check(createNewFile()) {
         "The image file could not be created."
      }
   }
}

private const val IMAGE_DIRECTORY_NAME = "UsedCarsRoom3"
private const val JPEG_EXTENSION = ".jpg"
private const val PNG_EXTENSION = ".png"
