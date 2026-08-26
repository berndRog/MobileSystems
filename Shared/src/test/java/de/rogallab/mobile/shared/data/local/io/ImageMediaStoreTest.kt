package de.rogallab.mobile.shared.data.local.io

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
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
class ImageMediaStoreTest {

   private val context =
      ApplicationProvider.getApplicationContext<Application>()

   private val mediaStore = ImageMediaStore(
      context = context,
      ioDispatcher = Dispatchers.Unconfined,
   )

   private fun createSourceFile(extension: String = "jpg"): File =
      File(
         context.cacheDir,
         "media-store-${UUID.randomUUID()}.$extension"
      ).apply {
         writeBytes(byteArrayOf(1, 2, 3))
      }

   @Test
   fun exportImageToMediaStore_missingSource_returnsFailure() = runTest {
      val missingFile = File(context.cacheDir, "missing-${UUID.randomUUID()}.jpg")

      val result = mediaStore.exportImageToMediaStore(
         imagePath = missingFile.absolutePath,
         groupName = "MobileSystems",
      )

      assertTrue(result.isFailure)
   }

   @Test
   fun exportImageToMediaStore_blankGroupName_returnsFailure() = runTest {
      val sourceFile = createSourceFile()

      val result = mediaStore.exportImageToMediaStore(
         imagePath = sourceFile.absolutePath,
         groupName = "   ",
      )

      assertTrue(result.isFailure)
      sourceFile.delete()
   }

   @Test
   fun exportImageToMediaStore_relativeParentSegment_returnsFailure() = runTest {
      val sourceFile = createSourceFile()

      val result = mediaStore.exportImageToMediaStore(
         imagePath = sourceFile.absolutePath,
         groupName = "People/../Private",
      )

      assertTrue(result.isFailure)
      sourceFile.delete()
   }

   @Test
   fun exportImageToMediaStore_unsupportedExtension_returnsFailure() = runTest {
      val sourceFile = createSourceFile(extension = "txt")

      val result = mediaStore.exportImageToMediaStore(
         imagePath = sourceFile.absolutePath,
         groupName = "MobileSystems",
      )

      assertTrue(result.isFailure)
      sourceFile.delete()
   }
}
