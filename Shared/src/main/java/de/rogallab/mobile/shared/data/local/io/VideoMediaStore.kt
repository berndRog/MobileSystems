package de.rogallab.mobile.shared.data.local.io

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import de.rogallab.mobile.shared.domain.images.IVideoMediaStore
import de.rogallab.mobile.shared.domain.images.PendingVideo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes videos into Android's shared MediaStore.
 *
 * Android 10 and newer use scoped storage with RELATIVE_PATH and IS_PENDING.
 * Android 9 and older use the legacy DATA column and require
 * WRITE_EXTERNAL_STORAGE at runtime.
 *
 * In contrast to ImageMediaStore the video is not copied from a private file.
 * The recorder writes directly into the MediaStore entry, which avoids storing
 * large video files twice.
 *
 * @param context Android context. The application context is retained internally.
 * @param ioDispatcher Dispatcher used for all MediaStore and file operations.
 */
class VideoMediaStore(
   context: Context,
   private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IVideoMediaStore {

   // Keep only the application context to avoid retaining an Activity
   // or another short-lived UI context.
   private val appContext = context.applicationContext

   override suspend fun createPendingVideo(
      groupName: String,
      fileName: String?,
   ): Result<PendingVideo> =
      withContext(ioDispatcher) {

         // Convert exceptions into Result.failure instead of exposing them directly.
         storageResult {

            // Validate and normalize the target directory below Movies.
            val normalizedGroupName = normalizeGroupName(groupName)

            // Use the requested name or generate a time-based one.
            val displayName = createDisplayName(fileName)

            // Android 10 introduced scoped storage and a different
            // way of publishing shared media files.
            val videoUri =
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                  insertScopedVideo(normalizedGroupName, displayName)
               else
                  insertLegacyVideo(normalizedGroupName, displayName)

            try {
               // "rw" is required: the MP4 muxer must seek in the file
               // to write the header after the recording has finished.
               val descriptor = appContext.contentResolver
                  .openFileDescriptor(videoUri, "rw")
                  ?: error("The MediaStore video could not be opened: $videoUri")

               PendingVideo(
                  uri = videoUri,
                  fileDescriptor = descriptor,
               )
            }
            catch (throwable: Throwable) {
               // Remove the empty entry when it cannot be opened for writing.
               appContext.contentResolver.delete(videoUri, null, null)
               throw throwable
            }
         }
      }

   override suspend fun publishVideo(videoUri: Uri): Result<Uri> =
      withContext(ioDispatcher) {
         storageResult {

            // Only scoped storage knows hidden entries. Legacy entries are
            // visible as soon as they have been inserted.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

               // Mark the video as complete so that it becomes visible
               // to other apps and the system gallery.
               val completedValues = ContentValues().apply {
                  put(
                     MediaStore.Video.Media.IS_PENDING,
                     0,
                  )
               }

               check(
                  appContext.contentResolver.update(
                     videoUri,
                     completedValues,
                     null,
                     null,
                  ) > 0
               ) {
                  "The MediaStore video could not be published."
               }
            }

            videoUri
         }
      }

   override suspend fun deleteVideoFromMediaStore(videoUri: Uri): Result<Unit> =
      withContext(ioDispatcher) {
         storageResult {

            // Delete the MediaStore entry identified by its content Uri.
            // The media provider also removes the underlying file.
            appContext.contentResolver.delete(
               videoUri,
               null,
               null,
            )

            Unit
         }
      }

   // Creates a hidden video entry using scoped storage on Android 10 and newer.
   private fun insertScopedVideo(
      groupName: String,
      displayName: String,
   ): Uri {

      // Use the primary shared external video collection.
      val collection = MediaStore.Video.Media.getContentUri(
         MediaStore.VOLUME_EXTERNAL_PRIMARY,
      )

      // Describe the new MediaStore entry before inserting it.
      val contentValues = ContentValues().apply {
         put(
            MediaStore.Video.Media.DISPLAY_NAME,
            displayName,
         )
         put(
            MediaStore.Video.Media.MIME_TYPE,
            VIDEO_MIME_TYPE,
         )
         put(
            MediaStore.Video.Media.RELATIVE_PATH,
            "${Environment.DIRECTORY_MOVIES}/$groupName",
         )
         put(
            MediaStore.Video.Media.DATE_TAKEN,
            System.currentTimeMillis(),
         )

         // Keep the file hidden from other apps while it is still being written.
         put(
            MediaStore.Video.Media.IS_PENDING,
            1,
         )
      }

      // Create an empty MediaStore entry and receive its content Uri.
      return appContext.contentResolver.insert(
         collection,
         contentValues,
      ) ?: error(
         "The MediaStore video could not be created."
      )
   }

   // Creates a video entry using the legacy shared-storage model
   // required on Android 9 and older.
   @Suppress("DEPRECATION")
   private fun insertLegacyVideo(
      groupName: String,
      displayName: String,
   ): Uri {

      // Build the public directory below the shared Movies directory.
      val publicDirectory = File(
         Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES,
         ),
         groupName,
      ).apply {
         check(exists() || mkdirs()) {
            "The public video directory could not be created: $absolutePath"
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
            MediaStore.Video.Media.DISPLAY_NAME,
            targetFile.name,
         )
         put(
            MediaStore.Video.Media.MIME_TYPE,
            VIDEO_MIME_TYPE,
         )
         put(
            MediaStore.Video.Media.DATA,
            targetFile.absolutePath,
         )
         put(
            MediaStore.Video.Media.DATE_TAKEN,
            System.currentTimeMillis(),
         )
      }

      // Register the legacy video in MediaStore.
      return appContext.contentResolver.insert(
         MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
         contentValues,
      ) ?: error(
         "The legacy MediaStore video could not be created."
      )
   }

   // Creates the display name used by MediaStore.
   // The extension is always ".mp4", because the recorder writes MPEG-4 files.
   private fun createDisplayName(fileName: String?): String {

      // Use the requested name when available.
      // File(...).name removes possible directory components.
      val requestedName = fileName
         ?.takeUnless(String::isBlank)
         ?.let { requestedFileName ->
            File(requestedFileName).name
         }
         ?: "VID_${timestamp()}"

      // Remove an existing extension before applying the required one.
      val baseName = requestedName.substringBeforeLast(
         delimiter = '.',
         missingDelimiterValue = requestedName,
      )

      return "$baseName$VIDEO_EXTENSION"
   }

   // Normalizes and validates a directory name below the shared Movies directory.
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

   // Time-based part of generated file names.
   private fun timestamp(): String =
      SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
         .format(System.currentTimeMillis())

   companion object {
      private const val VIDEO_MIME_TYPE = "video/mp4"
      private const val VIDEO_EXTENSION = ".mp4"
   }
}

/*
 * Didaktik / Lernziele:
 *
 * - Videos sind groß. Der Weg "erst in eine private Datei aufnehmen, dann in den
 *   MediaStore kopieren" (wie bei ImageMediaStore) würde den Speicherbedarf
 *   verdoppeln. Deshalb schreibt der Recorder hier direkt in den MediaStore-
 *   Eintrag. Die Studierenden sehen, dass die Wahl der Strategie von der
 *   Datengröße abhängt.
 *
 * - IS_PENDING wird hier in seiner eigentlichen Stärke genutzt: Der Eintrag
 *   existiert bereits, ist aber für andere Apps unsichtbar, solange die
 *   Aufnahme läuft. Erst publishVideo() macht ihn sichtbar. Eine halb
 *   geschriebene Aufnahme taucht so nie in der Galerie auf.
 *
 * - Das Schreiben ist ein Vorgang in drei Schritten, die an unterschiedlichen
 *   Stellen passieren:
 *   createPendingVideo()      -> Eintrag anlegen und Descriptor öffnen
 *   Aufnahme (CameraX)        -> Daten schreiben
 *   publishVideo() / delete   -> veröffentlichen oder verwerfen
 *   Der Zustand dazwischen wird in PendingVideo gehalten.
 *
 * - ParcelFileDescriptor ist das Android-Gegenstück zu einem geöffneten
 *   Dateihandle. Er entkoppelt die Domain von CameraX: Die Schnittstelle
 *   kennt weder MediaStoreOutputOptions noch andere CameraX-Typen. Jede
 *   Bibliothek, die in einen Descriptor schreiben kann, ist einsetzbar.
 *
 * - Der Modus "rw" ist nötig, weil der MP4-Container beim Beenden der Aufnahme
 *   zurückspringt und den Header schreibt. Ein nur schreibbarer, nicht
 *   suchbarer Datenstrom reicht dafür nicht aus.
 *
 * - PendingVideo implementiert Closeable. Das Schließen des Descriptors gehört
 *   zu dem, der ihn benutzt, und geschieht erst nach dem Finalize-Ereignis der
 *   Aufnahme.
 *
 * - Fehlerfälle werden aktiv aufgeräumt: Kann der Eintrag nicht geöffnet werden,
 *   wird er sofort gelöscht. Schlägt die Aufnahme fehl, löscht der Aufrufer den
 *   Eintrag. So bleiben keine leeren Videos zurück.
 *
 * - Android 9 und älter kennen weder RELATIVE_PATH noch IS_PENDING. Der
 *   Eintrag ist dort sofort sichtbar, publishVideo() hat in diesem Fall nichts
 *   zu tun. Die Kapselung verbirgt den Unterschied vor den aufrufenden Schichten.
 *
 * - Dependency Inversion: ViewModel und UseCases kennen nur IVideoMediaStore.
 *   Context, ContentResolver, ContentValues und Environment bleiben in der
 *   Data-Schicht. Das ViewModel lässt sich mit einem Fake testen.
 *
 * - Result<T> und storageResult machen Erfolg und Fehler zu einem Teil der
 *   Schnittstelle, wie bei IImageFileStorage und IImageMediaStore.
 */