package de.rogallab.mobile.shared.data.local.io

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageFileStorageInstrumentedTest {

   private val context =
      ApplicationProvider.getApplicationContext<Context>()

   private fun newDirectoryName(): String =
      "instrumented-images-${UUID.randomUUID()}"

   private fun newStorage(directoryName: String): ImageFileStorage =
      ImageFileStorage(
         context = context,
         directoryName = directoryName,
         ioDispatcher = Dispatchers.IO,
      )

   @Test
   fun createCameraImageFile_createsPrivateFileAndContentUri() = runTest {
      val directoryName = newDirectoryName()
      val storage = newStorage(directoryName)

      val cameraImage = storage
         .createCameraImageFile()
         .getOrThrow()

      val imageFile = File(cameraImage.imagePath)

      assertTrue(imageFile.isFile)
      assertEquals(0L, imageFile.length())
      assertTrue(
         imageFile.canonicalPath.startsWith(
            File(context.filesDir, directoryName).canonicalPath
         )
      )
      assertEquals("content", cameraImage.contentUri.scheme)

      storage.deleteImageFromAppStorage(cameraImage.imagePath)
   }

   @Test
   fun confirmCameraImageFile_emptyThenWrittenFile_changesFromFailureToSuccess() = runTest {
      val storage = newStorage(newDirectoryName())
      val cameraImage = storage
         .createCameraImageFile()
         .getOrThrow()

      val emptyResult =
         storage.confirmCameraImageFile(cameraImage.imagePath)

      assertTrue(emptyResult.isFailure)

      val expectedBytes = byteArrayOf(1, 2, 3, 4)
      File(cameraImage.imagePath).writeBytes(expectedBytes)

      val confirmedResult =
         storage.confirmCameraImageFile(cameraImage.imagePath)

      assertTrue(confirmedResult.isSuccess)

      // Android may expose the same private file through aliases such as
      // /data/user/0/... and /data/data/.... Verify the returned file instead
      // of comparing those platform-dependent path strings.
      val confirmedFile = File(confirmedResult.getOrThrow())
      assertTrue(confirmedFile.isFile)
      assertEquals(File(cameraImage.imagePath).name, confirmedFile.name)
      assertTrue(expectedBytes.contentEquals(confirmedFile.readBytes()))

      storage.deleteImageFromAppStorage(cameraImage.imagePath)
   }

   @Test
   fun confirmCameraImageFile_fileOutsideConfiguredDirectory_returnsFailure() = runTest {
      val storage = newStorage(newDirectoryName())
      val outsideFile = File(
         context.filesDir,
         "outside-${UUID.randomUUID()}.jpg"
      ).apply {
         writeBytes(byteArrayOf(1, 2, 3))
      }

      val result =
         storage.confirmCameraImageFile(outsideFile.absolutePath)

      assertTrue(result.isFailure)
      outsideFile.delete()
   }

   @Test
   fun copyImageToAppStorage_fileProviderUri_copiesBytesToNewPrivateFile() = runTest {
      val storage = newStorage(newDirectoryName())
      val source = storage
         .createCameraImageFile()
         .getOrThrow()
      val sourceBytes = byteArrayOf(10, 20, 30, 40, 50)
      File(source.imagePath).writeBytes(sourceBytes)

      val copiedPath = storage
         .copyImageToAppStorage(source.contentUri)
         .getOrThrow()

      val copiedFile = File(copiedPath)

      assertTrue(copiedFile.isFile)
      assertNotEquals(source.imagePath, copiedPath)
      assertTrue(sourceBytes.contentEquals(copiedFile.readBytes()))

      storage.deleteImageFromAppStorage(source.imagePath)
      storage.deleteImageFromAppStorage(copiedPath)
   }

   @Test
   fun saveDrawableToAppStorage_createsRequestedImageFormat() = runTest {
      val storage = newStorage(newDirectoryName())

      val imagePath = storage
         .saveDrawableToAppStorage(
            drawableResId = android.R.drawable.ic_menu_gallery,
            fileName = "seed-image.anything",
            format = ImageFileFormat.Png,
            quality = 100,
         )
         .getOrThrow()

      val imageFile = File(imagePath)

      assertTrue(imageFile.isFile)
      assertTrue(imageFile.length() > 0L)
      assertTrue(imageFile.name.endsWith(".png"))

      storage.deleteImageFromAppStorage(imagePath)
   }

   @Test
   fun saveDrawableToAppStorage_invalidQualityOrPath_returnsFailure() = runTest {
      val storage = newStorage(newDirectoryName())

      val qualityResult = storage.saveDrawableToAppStorage(
         drawableResId = android.R.drawable.ic_menu_gallery,
         fileName = "image.png",
         format = ImageFileFormat.Png,
         quality = 101,
      )

      val pathResult = storage.saveDrawableToAppStorage(
         drawableResId = android.R.drawable.ic_menu_gallery,
         fileName = "subdir/image.png",
         format = ImageFileFormat.Png,
         quality = 100,
      )

      assertTrue(qualityResult.isFailure)
      assertTrue(pathResult.isFailure)
   }

   @Test
   fun deleteImageFromAppStorage_existingAndMissingPaths_areIdempotent() = runTest {
      val storage = newStorage(newDirectoryName())
      val cameraImage = storage
         .createCameraImageFile()
         .getOrThrow()

      assertTrue(File(cameraImage.imagePath).exists())

      assertTrue(
         storage.deleteImageFromAppStorage(cameraImage.imagePath).isSuccess
      )
      assertFalse(File(cameraImage.imagePath).exists())

      assertTrue(
         storage.deleteImageFromAppStorage(cameraImage.imagePath).isSuccess
      )
      assertTrue(storage.deleteImageFromAppStorage(null).isSuccess)
      assertTrue(storage.deleteImageFromAppStorage("   ").isSuccess)
   }
}
