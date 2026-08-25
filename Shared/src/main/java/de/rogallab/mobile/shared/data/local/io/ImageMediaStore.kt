package de.rogallab.mobile.shared.data.local.io

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import de.rogallab.mobile.shared.domain.io.IImageMediaStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Exports private image files to Android's shared MediaStore.
 *
 * Android 10 and newer use scoped storage with RELATIVE_PATH and IS_PENDING.
 * Android 9 and older use the legacy DATA column and require
 * WRITE_EXTERNAL_STORAGE at runtime.
 *
 * @param context Android context. The application context is retained internally.
 * @param ioDispatcher Dispatcher used for all MediaStore and file operations.
 */
class ImageMediaStore(
   context: Context,
   private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IImageMediaStore {

   // Keep only the application context to avoid retaining an Activity
   // or another short-lived UI context.
   private val appContext = context.applicationContext

   override suspend fun exportImageToMediaStore(
      imagePath: String,
      groupName: String,
      fileName: String?,
   ): Result<Uri> =
      withContext(ioDispatcher) {

         // Convert exceptions into Result.failure instead of exposing them directly.
         storageResult {

            // The source must be an existing private image file.
            val sourceFile = File(imagePath)

            check(sourceFile.isFile) {
               "The source image does not exist: $imagePath"
            }

            // Validate and normalize the target directory below Pictures.
            val normalizedGroupName = normalizeGroupName(groupName)

            // Use the requested name or fall back to the source file name.
            val displayName = createDisplayName(
               sourceFile = sourceFile,
               fileName = fileName,
            )

            // MediaStore requires a MIME type describing the exported image.
            val mimeType = determineMimeType(sourceFile)

            // Android 10 introduced scoped storage and a different
            // way of publishing shared media files.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
               exportScopedImage(
                  sourceFile = sourceFile,
                  groupName = normalizedGroupName,
                  displayName = displayName,
                  mimeType = mimeType,
               )
            else
               exportLegacyImage(
                  sourceFile = sourceFile,
                  groupName = normalizedGroupName,
                  displayName = displayName,
                  mimeType = mimeType,
               )
         }
      }

   override suspend fun deleteImageFromMediaStore(imageUri: Uri): Result<Unit> =
      withContext(ioDispatcher) {
         storageResult {

            // Delete the MediaStore entry identified by its content URI.
            appContext.contentResolver.delete(
               imageUri,
               null,
               null,
            )

            Unit
         }
      }

   // Exports an image using scoped storage on Android 10 and newer.
   private fun exportScopedImage(
      sourceFile: File,
      groupName: String,
      displayName: String,
      mimeType: String,
   ): Uri {

      val contentResolver = appContext.contentResolver

      // Use the primary shared external image collection.
      val collection = MediaStore.Images.Media.getContentUri(
         MediaStore.VOLUME_EXTERNAL_PRIMARY,
      )

      // Describe the new MediaStore entry before inserting it.
      val contentValues = ContentValues().apply {
         put(
            MediaStore.Images.Media.DISPLAY_NAME,
            displayName,
         )
         put(
            MediaStore.Images.Media.MIME_TYPE,
            mimeType,
         )
         put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/$groupName",
         )
         put(
            MediaStore.Images.Media.DATE_TAKEN,
            System.currentTimeMillis(),
         )

         // Keep the file hidden from other apps while it is still being written.
         put(
            MediaStore.Images.Media.IS_PENDING,
            1,
         )
      }

      // Create an empty MediaStore entry and receive its content URI.
      val imageUri = contentResolver.insert(
         collection,
         contentValues,
      ) ?: error(
         "The MediaStore image could not be created."
      )

      try {
         // Copy the bytes from private app storage into the MediaStore entry.
         copyFileToUri(
            sourceFile = sourceFile,
            targetUri = imageUri,
         )

         // Mark the image as complete so that it becomes visible
         // to other apps and the system gallery.
         val completedValues = ContentValues().apply {
            put(
               MediaStore.Images.Media.IS_PENDING,
               0,
            )
         }

         check(
            contentResolver.update(
               imageUri,
               completedValues,
               null,
               null,
            ) > 0
         ) {
            "The exported MediaStore image could not be published."
         }

         return imageUri
      }
      catch (throwable: Throwable) {
         // Remove incomplete MediaStore entries when exporting fails.
         contentResolver.delete(
            imageUri,
            null,
            null,
         )
         throw throwable
      }
   }

   // Exports an image using the legacy shared-storage model
   // required on Android 9 and older.
   @Suppress("DEPRECATION")
   private fun exportLegacyImage(
      sourceFile: File,
      groupName: String,
      displayName: String,
      mimeType: String,
   ): Uri {

      // Build the public directory below the shared Pictures directory.
      val publicDirectory = File(
         Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES,
         ),
         groupName,
      ).apply {
         check(exists() || mkdirs()) {
            "The public image directory could not be created: $absolutePath"
         }
      }

      // Avoid overwriting an existing file with the same name.
      val targetFile = createUniqueTargetFile(
         directory = publicDirectory,
         displayName = displayName,
      )

      // Legacy MediaStore uses the absolute DATA path instead of RELATIVE_PATH.
      val contentValues = ContentValues().apply {
         put(
            MediaStore.Images.Media.DISPLAY_NAME,
            targetFile.name,
         )
         put(
            MediaStore.Images.Media.MIME_TYPE,
            mimeType,
         )
         put(
            MediaStore.Images.Media.DATA,
            targetFile.absolutePath,
         )
         put(
            MediaStore.Images.Media.DATE_TAKEN,
            System.currentTimeMillis(),
         )
      }

      val contentResolver = appContext.contentResolver

      // Register the legacy image in MediaStore.
      val imageUri = contentResolver.insert(
         MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
         contentValues,
      ) ?: error(
         "The legacy MediaStore image could not be created."
      )

      try {
         // Copy the source image into the registered MediaStore destination.
         copyFileToUri(
            sourceFile = sourceFile,
            targetUri = imageUri,
         )

         return imageUri
      }
      catch (throwable: Throwable) {
         // Remove both the MediaStore entry and a possible incomplete file.
         contentResolver.delete(
            imageUri,
            null,
            null,
         )
         targetFile.delete()
         throw throwable
      }
   }

   // Copies a private source file into a target content URI.
   private fun copyFileToUri(
      sourceFile: File,
      targetUri: Uri,
   ) {

      appContext.contentResolver
         .openOutputStream(
            targetUri,
            "w",
         )
         ?.use { outputStream ->

            // Both streams are closed automatically after copying.
            sourceFile.inputStream().use { inputStream ->
               inputStream.copyTo(outputStream)
            }
         }
         ?: error(
            "The MediaStore output stream could not be opened: $targetUri"
         )
   }

   // Determines the image MIME type from the file extension.
   private fun determineMimeType(sourceFile: File): String {

      val extension = sourceFile.extension.lowercase()

      return MimeTypeMap
         .getSingleton()
         .getMimeTypeFromExtension(extension)
         ?.takeIf { mimeType ->
            mimeType.startsWith("image/")
         }
         ?: error(
            "Unsupported image file extension: .${sourceFile.extension}"
         )
   }

   // Creates the display name used by MediaStore.
   // The original file extension is always retained.
   private fun createDisplayName(
      sourceFile: File,
      fileName: String?,
   ): String {

      // Use the requested name when available.
      // File(...).name removes possible directory components.
      val requestedName = fileName
         ?.takeUnless(String::isBlank)
         ?.let { requestedFileName ->
            File(requestedFileName).name
         }
         ?: sourceFile.name

      // Remove an existing extension before applying the source extension.
      val baseName = requestedName.substringBeforeLast(
         delimiter = '.',
         missingDelimiterValue = requestedName,
      )

      return "$baseName.${sourceFile.extension.lowercase()}"
   }

   // Normalizes and validates a directory name below the shared Pictures directory.
   private fun normalizeGroupName(groupName: String): String {

      // Support both slash styles and remove empty path segments.
      val pathParts = groupName
         .trim()
         .replace('\\', '/')
         .split('/')
         .filter(String::isNotBlank)

      require(pathParts.isNotEmpty()) {
         "The MediaStore group name must not be blank."
      }

      // Reject relative path segments that could leave the intended directory.
      require(
         pathParts.none { pathPart ->
            pathPart == "." || pathPart == ".."
         }
      ) {
         "The MediaStore group name contains an invalid path segment."
      }

      // Rebuild a normalized relative path.
      return pathParts.joinToString("/")
   }

   // Finds a free file name without overwriting an existing legacy file.
   private fun createUniqueTargetFile(
      directory: File,
      displayName: String,
   ): File {

      val requestedFile = File(
         directory,
         displayName,
      )

      if (!requestedFile.exists())
         return requestedFile

      val baseName = requestedFile.nameWithoutExtension
      val extension = requestedFile.extension

      var index = 2

      // Add a numeric suffix until an unused file name is found.
      while (true) {
         val candidate = File(
            directory,
            "${baseName}_$index.$extension",
         )

         if (!candidate.exists())
            return candidate

         index++
      }
   }
}

/*
 * Didaktik / Lernziele:
 *
 * - Private App-Dateien und gemeinsam sichtbare Mediendateien sind zwei
 *   unterschiedliche Speicherbereiche. ImageFileStorage verwaltet private
 *   Dateien, während ImageMediaStore den bewussten Export in den gemeinsam
 *   genutzten MediaStore übernimmt.
 *
 * - Ein MediaStore-Eintrag wird nicht über einen normalen Dateipfad,
 *   sondern über einen Content-URI angesprochen.
 *
 * - ContentResolver bildet die zentrale Android-Schnittstelle zum Lesen,
 *   Schreiben, Einfügen, Ändern und Löschen von Content-URIs.
 *
 * - ContentValues beschreibt die Metadaten eines neuen MediaStore-Eintrags,
 *   beispielsweise Dateiname, MIME-Typ, Zielverzeichnis und Aufnahmezeit.
 *
 * - Android 10 führte Scoped Storage ein. Dateien werden seitdem über
 *   RELATIVE_PATH einem öffentlichen Verzeichnis zugeordnet, ohne dass die
 *   Anwendung selbst einen absoluten externen Dateipfad verwalten muss.
 *
 * - IS_PENDING ermöglicht ein zweistufiges Schreiben:
 *   Zunächst wird ein noch unsichtbarer MediaStore-Eintrag erzeugt,
 *   anschließend werden die Bilddaten geschrieben und erst danach wird
 *   die Datei veröffentlicht.
 *
 * - Ältere Android-Versionen verwenden dagegen das Legacy-Storage-Modell
 *   mit einem absoluten Dateipfad in der DATA-Spalte. Der Versionsvergleich
 *   zeigt, wie unterschiedliche Plattform-APIs gekapselt werden können.
 *
 * - MIME-Typ und Dateiendung erfüllen unterschiedliche Aufgaben.
 *   MediaStore benötigt den MIME-Typ, während der Dateiname weiterhin
 *   eine geeignete Dateiendung besitzt.
 *
 * - use sorgt dafür, dass InputStream und OutputStream zuverlässig
 *   geschlossen werden, auch wenn während des Kopierens ein Fehler auftritt.
 *
 * - try/catch wird genutzt, um bei fehlgeschlagenen Exporten bereits
 *   angelegte MediaStore-Einträge wieder zu entfernen. Dadurch bleiben
 *   keine unvollständigen Bilder zurück.
 *
 * - Die Hilfsfunktionen kapseln jeweils eine klar abgegrenzte Aufgabe:
 *   copyFileToUri()        -> Daten kopieren
 *   determineMimeType()    -> MIME-Typ bestimmen
 *   createDisplayName()    -> Dateinamen erzeugen
 *   normalizeGroupName()   -> Zielpfad prüfen
 *   createUniqueTargetFile() -> Namenskollisionen vermeiden
 *
 * - Die Trennung zwischen IImageMediaStore und ImageMediaStore zeigt erneut
 *   das Dependency-Inversion-Prinzip: Höhere Schichten hängen von einer
 *   Schnittstelle ab, während Android-spezifische Details in der konkreten
 *   Implementierung verbleiben.
 */
