package de.rogallab.mobile.shared.data.local.io

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageMediaStoreInstrumentedTest {

   private val context =
      ApplicationProvider.getApplicationContext<Context>()

   private val mediaStore = ImageMediaStore(
      context = context,
      ioDispatcher = Dispatchers.IO,
   )

   private fun createSourceFile(extension: String = "jpg"): File =
      File(
         context.cacheDir,
         "media-store-${UUID.randomUUID()}.$extension"
      ).apply {
         writeBytes(byteArrayOf(1, 2, 3, 4, 5))
      }

   @Test
   fun exportImageToMediaStore_missingSource_returnsFailure() = runTest {
      val missingFile = File(
         context.cacheDir,
         "missing-${UUID.randomUUID()}.jpg"
      )

      val result = mediaStore.exportImageToMediaStore(
         imagePath = missingFile.absolutePath,
         groupName = "MobileSystemsTests",
      )

      assertTrue(result.isFailure)
   }

   @Test
   fun exportImageToMediaStore_invalidGroupOrExtension_returnsFailure() = runTest {
      val jpgFile = createSourceFile()
      val txtFile = createSourceFile("txt")

      val blankGroupResult = mediaStore.exportImageToMediaStore(
         imagePath = jpgFile.absolutePath,
         groupName = "   ",
      )

      val parentSegmentResult = mediaStore.exportImageToMediaStore(
         imagePath = jpgFile.absolutePath,
         groupName = "People/../Private",
      )

      val extensionResult = mediaStore.exportImageToMediaStore(
         imagePath = txtFile.absolutePath,
         groupName = "MobileSystemsTests",
      )

      assertTrue(blankGroupResult.isFailure)
      assertTrue(parentSegmentResult.isFailure)
      assertTrue(extensionResult.isFailure)

      jpgFile.delete()
      txtFile.delete()
   }

   @Test
   fun exportAndDeleteImage_scopedMediaStore_roundTripsEntry() = runTest {
      // The production implementation uses scoped storage from Android 10 on.
      // This test intentionally verifies that modern path on a real device/emulator.
      assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

      val sourceFile = createSourceFile()
      val displayName = "export-${UUID.randomUUID()}.jpg"

      val imageUri = mediaStore
         .exportImageToMediaStore(
            imagePath = sourceFile.absolutePath,
            groupName = "MobileSystemsTests",
            fileName = displayName,
         )
         .getOrThrow()

      assertEquals("content", imageUri.scheme)

      context.contentResolver.query(
         imageUri,
         arrayOf(MediaStore.Images.Media.DISPLAY_NAME),
         null,
         null,
         null,
      )!!.use { cursor ->
         assertTrue(cursor.moveToFirst())
         assertEquals(displayName, cursor.getString(0))
      }

      val deleteResult =
         mediaStore.deleteImageFromMediaStore(imageUri)

      assertTrue(deleteResult.isSuccess)

      context.contentResolver.query(
         imageUri,
         arrayOf(MediaStore.Images.Media._ID),
         null,
         null,
         null,
      )!!.use { cursor ->
         assertEquals(0, cursor.count)
      }

      sourceFile.delete()
   }
}
