package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import de.rogallab.mobile.shared.domain.io.CameraImageFile
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEditDelegateTest {

   private val storage = FakeImageFileStorage()
   private val delegate = ImageEditDelegate(storage)

   @Test
   fun replace_normalizesBlankAndDuplicatePaths() = runTest {
      delegate.start(emptyList())

      val images = delegate.replace(
         listOf("/images/a.jpg", "", "/images/a.jpg", "   ")
      )

      assertEquals(listOf("/images/a.jpg"), images)
   }

   @Test
   fun replace_obsoleteUnsavedImageIsDeletedImmediately() = runTest {
      delegate.start(emptyList())
      delegate.replace(listOf("/images/one.jpg"))

      val images = delegate.replace(listOf("/images/two.jpg"))

      assertTrue("/images/one.jpg" in storage.deletedPaths)
      assertEquals(listOf("/images/two.jpg"), images)
   }

   @Test
   fun replace_persistedOriginalIsNotDeletedBeforeCommit() = runTest {
      delegate.start(listOf("/images/original.jpg"))

      delegate.replace(listOf("/images/new.jpg"))

      assertFalse("/images/original.jpg" in storage.deletedPaths)
   }

   @Test
   fun commit_deletesPersistedOriginalThatIsNoLongerSelected() = runTest {
      delegate.start(listOf("/images/original.jpg"))
      delegate.replace(listOf("/images/new.jpg"))

      delegate.commit()

      assertTrue("/images/original.jpg" in storage.deletedPaths)
      assertFalse("/images/new.jpg" in storage.deletedPaths)
   }

   @Test
   fun commit_makesCurrentSelectionTheNewOriginal() = runTest {
      delegate.start(listOf("/images/original.jpg"))
      delegate.replace(listOf("/images/new.jpg"))
      delegate.commit()

      delegate.discard()

      assertFalse("/images/new.jpg" in storage.deletedPaths)
   }

   @Test
   fun discard_deletesOnlyImagesCreatedDuringCurrentSession() = runTest {
      delegate.start(listOf("/images/original.jpg"))
      delegate.replace(listOf("/images/original.jpg", "/images/new.jpg"))

      delegate.discard()

      assertTrue("/images/new.jpg" in storage.deletedPaths)
      assertFalse("/images/original.jpg" in storage.deletedPaths)
   }

   @Test
   fun add_appendsAndNormalizesImages() = runTest {
      delegate.start(listOf("/images/a.jpg"))

      val images = delegate.add(
         listOf("/images/b.jpg", "/images/a.jpg", "")
      )

      assertEquals(
         listOf("/images/a.jpg", "/images/b.jpg"),
         images
      )
   }

   @Test
   fun remove_unsavedImageIsDeletedImmediately() = runTest {
      delegate.start(listOf("/images/original.jpg"))
      delegate.add(listOf("/images/new.jpg"))

      val images = delegate.remove("/images/new.jpg")

      assertEquals(listOf("/images/original.jpg"), images)
      assertTrue("/images/new.jpg" in storage.deletedPaths)
   }

   @Test
   fun remove_persistedImageIsDeletedOnlyAfterCommit() = runTest {
      delegate.start(
         listOf("/images/a.jpg", "/images/b.jpg")
      )

      val images = delegate.remove("/images/a.jpg")

      assertEquals(listOf("/images/b.jpg"), images)
      assertFalse("/images/a.jpg" in storage.deletedPaths)

      delegate.commit()

      assertTrue("/images/a.jpg" in storage.deletedPaths)
   }

   private class FakeImageFileStorage : IImageFileStorage {
      val deletedPaths = mutableListOf<String?>()

      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun createCameraImageFile(): Result<CameraImageFile> =
         Result.failure(UnsupportedOperationException())

      override suspend fun confirmCameraImageFile(imagePath: String): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun saveDrawableToAppStorage(
         drawableResId: Int,
         fileName: String,
         format: ImageFileFormat,
         quality: Int,
      ): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun deleteImageFromAppStorage(
         imagePath: String?
      ): Result<Unit> {
         deletedPaths += imagePath
         return Result.success(Unit)
      }
   }
}
