package de.rogallab.mobile.shared.data.local.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import de.rogallab.mobile.shared.domain.io.CameraImageFile
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores images in a configurable subdirectory of the app's private files area.
 *
 * The class encapsulates Android-specific file access and implements the
 * platform-independent IImageFileStorage interface used by higher layers.
 *
 * @param context Android context. The application context is retained internally.
 * @param directoryName Subdirectory below Context.filesDir.
 * @param ioDispatcher Dispatcher used for all file operations.
 * @param fileProviderAuthority Authority declared by the library FileProvider.
 */
class ImageFileStorage(
   context: Context,
   private val directoryName: String,
   private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
   private val fileProviderAuthority: String = "${context.packageName}.fileprovider",
) : IImageFileStorage {

   // Keep only the application context to avoid retaining an Activity or other UI context.
   private val appContext = context.applicationContext

   override suspend fun copyImageToAppStorage(
      sourceUri: Uri
   ): Result<String> =
      withContext(ioDispatcher) {

         // Convert exceptions into Result.failure instead of exposing them directly.
         storageResult {

            // Create the target file before copying the source content.
            val targetFile = createImageFile(
               extension = determineSourceExtension(sourceUri),
            )

            try {
               // Open the content URI and copy its bytes into private app storage.
               appContext.contentResolver
                  .openInputStream(sourceUri)
                  ?.use { inputStream ->
                     targetFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                     }
                  }
                  ?: error("The selected image could not be opened: $sourceUri")

               // A successful copy must produce a non-empty file.
               check(targetFile.length() > 0L) {
                  "The copied image file is empty: ${targetFile.absolutePath}"
               }

               // Room can store this absolute path without keeping URI permissions.
               targetFile.absolutePath
            }
            catch (throwable: Throwable) {
               // Remove incomplete files when copying fails.
               targetFile.delete()
               throw throwable
            }
         }
      }

   override suspend fun createCameraImageFile(): Result<CameraImageFile> =
      withContext(ioDispatcher) {
         storageResult {

            // Create an empty JPEG file that will be filled by the camera app.
            val imageFile = createImageFile(
               extension = JPEG_EXTENSION,
            )

            try {
               // Convert the private file into a content URI that can safely
               // be shared temporarily with an external camera application.
               val contentUri = FileProvider.getUriForFile(
                  appContext,
                  fileProviderAuthority,
                  imageFile,
               )

               CameraImageFile(
                  imagePath = imageFile.absolutePath,
                  contentUri = contentUri,
               )
            }
            catch (throwable: Throwable) {
               // Remove the provisional file if the URI cannot be created.
               imageFile.delete()
               throw throwable
            }
         }
      }

   override suspend fun confirmCameraImageFile(imagePath: String): Result<String> =
      withContext(ioDispatcher) {
         storageResult {

            // Resolve canonical paths to avoid accepting paths that escape
            // the configured image directory through ".." or symbolic links.
            val storageDirectory = createStorageDirectory().canonicalFile
            val imageFile = File(imagePath).canonicalFile

            // Accept only files located inside this storage directory.
            check(
               imageFile.path.startsWith(
                  "${storageDirectory.path}${File.separator}",
               )
            ) {
               "The camera image is outside the configured storage directory."
            }

            // Verify that the camera application actually created a file.
            check(imageFile.isFile) {
               "The camera image does not exist: $imagePath"
            }

            // A cancelled or failed camera operation may leave an empty file.
            check(imageFile.length() > 0L) {
               "The camera image is empty: $imagePath"
            }

            imageFile.absolutePath
         }
      }

   override suspend fun saveDrawableToAppStorage(
      drawableResId: Int,
      fileName: String,
      format: ImageFileFormat,
      quality: Int,
   ): Result<String> =
      withContext(ioDispatcher) {
         storageResult {

            // Bitmap compression accepts quality values between 0 and 100.
            require(quality in 0..100) {
               "Image quality must be between 0 and 100."
            }

            // Remove path components and enforce the extension required by the format.
            val safeFileName = normalizeFileName(
               fileName = fileName,
               requiredExtension = format.extension,
            )

            // Load the drawable from Android resources.
            val drawable = ContextCompat.getDrawable(
               appContext,
               drawableResId,
            ) ?: error(
               "Drawable resource $drawableResId could not be loaded."
            )

            // Drawables must be converted to a bitmap before they can be compressed.
            val bitmap = drawableToBitmap(drawable)

            val targetFile = File(
               createStorageDirectory(),
               safeFileName,
            )

            try {
               // Encode the bitmap using the requested image format.
               FileOutputStream(targetFile).use { outputStream ->
                  check(
                     bitmap.compress(
                        format.toBitmapCompressFormat(),
                        quality,
                        outputStream,
                     )
                  ) {
                     "The drawable could not be written to ${targetFile.absolutePath}."
                  }
               }

               targetFile.absolutePath
            }
            catch (throwable: Throwable) {
               // Do not keep partially written image files.
               targetFile.delete()
               throw throwable
            }
         }
      }

   override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
      withContext(ioDispatcher) {
         storageResult {

            // Null and blank paths mean that there is nothing to delete.
            val path = imagePath?.takeUnless(String::isBlank)
               ?: return@storageResult Unit

            val imageFile = File(path)

            // Deleting a missing file is treated as success.
            check(!imageFile.exists() || imageFile.delete()) {
               "The image file could not be deleted: $path"
            }
         }
      }

   // Creates a uniquely named empty image file in the private image directory.
   private fun createImageFile(extension: String): File =
      File(
         createStorageDirectory(),
         "${UUID.randomUUID()}$extension",
      ).apply {
         check(createNewFile()) {
            "The image file could not be created: $absolutePath"
         }
      }

   // Returns the configured private image directory and creates it when necessary.
   private fun createStorageDirectory(): File =
      File(
         appContext.filesDir,
         directoryName,
      ).apply {
         check(exists() || mkdirs()) {
            "The image directory could not be created: $absolutePath"
         }
      }

   // Determines a useful file extension for content selected through a content URI.
   private fun determineSourceExtension(sourceUri: Uri): String {

      // Prefer the MIME type provided by the ContentResolver.
      val mimeType = appContext.contentResolver.getType(sourceUri)

      if (mimeType != null)
         require(mimeType.startsWith("image/")) {
            "The selected content is not an image: $mimeType"
         }

      // Convert the MIME type into a conventional file extension.
      val mimeExtension = mimeType
         ?.let { imageMimeType ->
            MimeTypeMap
               .getSingleton()
               .getExtensionFromMimeType(imageMimeType)
         }
         ?.normalizeExtension()

      if (mimeExtension != null)
         return mimeExtension

      // If no MIME extension is available, try to derive one from the URI path.
      val pathExtension = sourceUri.lastPathSegment
         ?.substringAfterLast(
            '.',
            missingDelimiterValue = "",
         )
         ?.takeIf { extension ->
            extension.length in 2..5 &&
               extension.all { character ->
                  character.isLetterOrDigit()
               }
         }
         ?.normalizeExtension()

      // Use a neutral fallback when neither MIME type nor path reveals the format.
      return pathExtension ?: IMAGE_EXTENSION
   }

   // Prevents callers from supplying directory paths and applies
   // the extension required by the selected image format.
   private fun normalizeFileName(
      fileName: String,
      requiredExtension: String,
   ): String {

      // File(...).name keeps only the final file-name component.
      val safeName = File(fileName).name

      require(safeName.isNotBlank()) {
         "The image file name must not be blank."
      }

      // Reject file names containing path components instead of silently accepting them.
      require(safeName == fileName) {
         "The image file name must not contain a path."
      }

      // Remove an existing extension before adding the required one.
      val baseName = safeName.substringBeforeLast(
         '.',
         missingDelimiterValue = safeName,
      )

      return "$baseName$requiredExtension"
   }

   // Converts any Drawable into a Bitmap that can be written to an image file.
   private fun drawableToBitmap(drawable: Drawable): Bitmap {

      // Reuse the existing bitmap when the drawable already contains one.
      if (drawable is BitmapDrawable && drawable.bitmap != null)
         return drawable.bitmap

      // Vector and other drawables are rendered onto a newly created bitmap.
      val bitmap = Bitmap.createBitmap(
         drawable.intrinsicWidth.coerceAtLeast(1),
         drawable.intrinsicHeight.coerceAtLeast(1),
         Bitmap.Config.ARGB_8888,
      )

      val canvas = Canvas(bitmap)

      drawable.setBounds(
         0,
         0,
         canvas.width,
         canvas.height,
      )
      drawable.draw(canvas)

      return bitmap
   }

   // Maps the domain image format to Android's bitmap compression format.
   private fun ImageFileFormat.toBitmapCompressFormat(): Bitmap.CompressFormat =
      when (this) {
         ImageFileFormat.Jpeg ->
            Bitmap.CompressFormat.JPEG

         ImageFileFormat.Png ->
            Bitmap.CompressFormat.PNG

         ImageFileFormat.Webp ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
               Bitmap.CompressFormat.WEBP_LOSSLESS
            else {
               @Suppress("DEPRECATION")
               Bitmap.CompressFormat.WEBP
            }
      }

   // Converts extensions to lowercase and uses ".jpg" consistently for JPEG files.
   private fun String.normalizeExtension(): String {
      val normalized = lowercase()

      return when (normalized) {
         "jpeg" -> JPEG_EXTENSION
         else -> ".$normalized"
      }
   }

   companion object {
      private const val JPEG_EXTENSION = ".jpg"

      // Used only when the actual source format cannot be determined.
      private const val IMAGE_EXTENSION = ".img"
   }
}

/*
 * Didaktik / Lernziele:
 *
 * - Die Schnittstelle IImageFileStorage trennt die fachlich benötigten
 *   Dateioperationen von ihrer Android-spezifischen Implementierung.
 *
 * - Android-Komponenten wie Context, ContentResolver, FileProvider,
 *   Drawable und Bitmap bleiben dadurch in der Data-/Infrastructure-Schicht.
 *
 * - Dateioperationen sind blockierende I/O-Operationen und werden deshalb
 *   mit withContext(ioDispatcher) außerhalb des Main Threads ausgeführt.
 *
 * - Result<T> macht Erfolg und Fehler zu einem expliziten Bestandteil der
 *   Schnittstelle. Aufrufer müssen technische Exceptions dadurch nicht
 *   unmittelbar behandeln.
 *
 * - use schließt Streams automatisch und demonstriert den sicheren Umgang
 *   mit Ressourcen.
 *
 * - try/catch wird zusätzlich genutzt, um bei Fehlern bereits erzeugte oder
 *   teilweise beschriebene Dateien wieder zu entfernen.
 *
 * - Content-URIs und Dateipfade erfüllen unterschiedliche Aufgaben:
 *   Ein Content-URI wird zum kontrollierten Austausch mit anderen Android-Apps
 *   verwendet, während der absolute Pfad für private App-Dateien gespeichert
 *   werden kann.
 *
 * - FileProvider ermöglicht es, einer Kamera-App temporär Zugriff auf eine
 *   private Datei zu geben, ohne einen file://-URI offenzulegen.
 *
 * - canonicalFile und die Prüfung des Speicherverzeichnisses zeigen, dass
 *   Dateipfade vor ihrer Verwendung validiert werden sollten.
 *
 * - MIME-Typ und Dateiendung sind nicht identisch. Wenn möglich wird deshalb
 *   zuerst der vom ContentResolver gelieferte MIME-Typ ausgewertet.
 *
 * - Helper-Funktionen wie createImageFile(), determineSourceExtension(),
 *   drawableToBitmap() und normalizeFileName() zerlegen eine umfangreichere
 *   Infrastrukturaufgabe in kleine, klar abgegrenzte Operationen.
 *
 * - Die Implementierung demonstriert außerdem Dependency Injection:
 *   Context, directoryName und CoroutineDispatcher werden von außen
 *   bereitgestellt und nicht innerhalb der Klasse erzeugt.
 */