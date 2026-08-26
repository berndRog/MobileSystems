package de.rogallab.mobile.shared.data.local.io

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class ImageFileStorageTest {

   private val context =
      ApplicationProvider.getApplicationContext<Application>()

   private fun newStorage(directoryName: String): ImageFileStorage =
      ImageFileStorage(
         context = context,
         directoryName = directoryName,
         ioDispatcher = Dispatchers.Unconfined,
      )

   private fun newDirectoryName(): String =
      "image-storage-test-${UUID.randomUUID()}"

   @Test
   fun confirmCameraImageFile_nonEmptyFileInsideStorage_returnsCanonicalPath() =
      runTest {
         val directoryName = newDirectoryName()
         val storage = newStorage(directoryName)
         val directory = File(context.filesDir, directoryName).apply { mkdirs() }
         val imageFile = File(directory, "camera.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
         }

         val result = storage.confirmCameraImageFile(imageFile.absolutePath)

         assertTrue(result.isSuccess)
         assertEquals(imageFile.canonicalPath, result.getOrNull())
      }

   @Test
   fun confirmCameraImageFile_emptyFile_returnsFailure() =
      runTest {
         val directoryName = newDirectoryName()
         val storage = newStorage(directoryName)
         val directory = File(context.filesDir, directoryName).apply { mkdirs() }
         val imageFile = File(directory, "camera.jpg").apply { createNewFile() }

         val result = storage.confirmCameraImageFile(imageFile.absolutePath)

         assertTrue(result.isFailure)
      }

   @Test
   fun confirmCameraImageFile_missingFile_returnsFailure() =
      runTest {
         val directoryName = newDirectoryName()
         val storage = newStorage(directoryName)
         val missingFile = File(
            File(context.filesDir, directoryName),
            "missing.jpg"
         )

         val result = storage.confirmCameraImageFile(missingFile.absolutePath)

         assertTrue(result.isFailure)
      }

   @Test
   fun confirmCameraImageFile_fileOutsideStorage_returnsFailure() =
      runTest {
         val directoryName = newDirectoryName()
         val storage = newStorage(directoryName)
         val outsideFile = File(context.filesDir, "outside.jpg").apply {
            writeBytes(byteArrayOf(1))
         }

         val result = storage.confirmCameraImageFile(outsideFile.absolutePath)

         assertTrue(result.isFailure)
         outsideFile.delete()
      }

   @Test
   fun deleteImageFromAppStorage_nullBlankAndMissingPaths_returnSuccess() =
      runTest {
         val storage = newStorage(newDirectoryName())

         assertTrue(storage.deleteImageFromAppStorage(null).isSuccess)
         assertTrue(storage.deleteImageFromAppStorage("").isSuccess)
         assertTrue(storage.deleteImageFromAppStorage("   ").isSuccess)
         assertTrue(
            storage.deleteImageFromAppStorage(
               File(context.filesDir, "does-not-exist.jpg").absolutePath
            ).isSuccess
         )
      }

   @Test
   fun deleteImageFromAppStorage_existingFile_deletesFile() =
      runTest {
         val storage = newStorage(newDirectoryName())
         val imageFile = File(context.filesDir, "delete-me.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
         }

         val result = storage.deleteImageFromAppStorage(imageFile.absolutePath)

         assertTrue(result.isSuccess)
         assertFalse(imageFile.exists())
      }
}
