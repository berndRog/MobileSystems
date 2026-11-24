package de.rogallab.mobile.shared.ui.images

import android.app.Application
import android.net.Uri
import de.rogallab.mobile.shared.domain.images.CameraImageFile
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.domain.images.ImageFileFormat
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ImageEditProcessingTest {
   private val original = "/images/original.jpg"
   private val photo = "/images/photo.jpg"
   private class Storage(original: String) : IImageFileStorage {
      val files = mutableSetOf(original)
      val copied = mutableListOf<Uri>()
      val deleted = mutableListOf<String>()
      var copy: suspend (Uri) -> Result<String> = {
         Result.success("/images/${it.lastPathSegment}.jpg")
      }
      var beforeDelete: suspend (String) -> Result<Unit> = { Result.success(Unit) }

      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> {
         copied += sourceUri
         return copy(sourceUri).onSuccess { files += it }
      }
      override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
         if (imagePath == null) return Result.success(Unit)
         deleted += imagePath
         return beforeDelete(imagePath).onSuccess { files -= imagePath }
      }
      override suspend fun createCameraImageFile(): Result<CameraImageFile> = error("not used")
      override suspend fun createPrivateImageFile(): Result<String> = error("not used")
      override suspend fun confirmCameraImageFile(imagePath: String): Result<String> = error("not used")
      override suspend fun saveDrawableToAppStorage(
         drawableResId: Int, fileName: String, format: ImageFileFormat, quality: Int,
      ): Result<String> = error("not used")
   }
   private val storage = Storage(original)
   private val session = ImageEdit(storage)
   private fun delegate(mode: ImageSelectionMode = ImageSelectionMode.APPEND, max: Int = 8) =
      ImageEditDelegate(session, storage, mode, max).also { it.start(listOf(original)) }
   private fun uri(name: String) = Uri.parse("content://images/$name")

   @Before fun setUp() { Alog.set(useAndroidLog = false) }
   @After fun tearDown() { Alog.reset() }

   @Test fun append_importsIntoExistingSelection() = runTest {
      val change = delegate().importGallery(listOf(uri("a"), uri("b"))).getOrThrow()
      assertEquals(ImageChange(listOf(original, "/images/a.jpg", "/images/b.jpg")), change)
      assertEquals(change.paths, session.add(emptyList()))
      assertTrue(storage.files.containsAll(change.paths!!))
   }

   @Test fun partialImport_returnsSelectionAndFailureCount() = runTest {
      storage.copy = {
         if (it == uri("bad")) Result.failure(IllegalStateException("unreadable"))
         else Result.success("/images/${it.lastPathSegment}.jpg")
      }
      val change = delegate().importGallery(listOf(uri("a"), uri("bad"), uri("b"))).getOrThrow()
      assertEquals(1, change.failedImports)
      assertEquals(listOf(original, "/images/a.jpg", "/images/b.jpg"), change.paths)
      assertEquals(change.paths, session.add(emptyList()))
   }

   @Test fun failedReplace_keepsExistingImage() = runTest {
      storage.copy = { Result.failure(IllegalStateException("unreadable")) }
      val result = delegate(ImageSelectionMode.REPLACE, 1).importGallery(listOf(uri("bad")))
      assertTrue(result.isFailure)
      assertEquals(listOf(original), session.add(emptyList()))
      assertEquals(setOf(original), storage.files)
      assertTrue(storage.deleted.isEmpty())
   }

   @Test fun replace_limitsToOneAndKeepsOriginalUntilCommit() = runTest {
      val change = delegate(ImageSelectionMode.REPLACE, 1)
         .importGallery(listOf(uri("a"), uri("b"))).getOrThrow()
      assertEquals(listOf("/images/a.jpg"), change.paths)
      assertTrue(original in storage.files)
      assertFalse("/images/b.jpg" in storage.files)
      session.commit()
      assertEquals(setOf("/images/a.jpg"), storage.files)
   }

   @Test fun partialReplace_usesSuccessfulImportInsteadOfClearingSelection() = runTest {
      storage.copy = {
         if (it == uri("bad")) Result.failure(IllegalStateException("unreadable"))
         else Result.success(photo)
      }
      val change = delegate(ImageSelectionMode.REPLACE, 1)
         .importGallery(listOf(uri("bad"), uri("a"))).getOrThrow()
      assertEquals(ImageChange(listOf(photo), 1), change)
      assertTrue(original in storage.files)
      assertTrue(photo in storage.files)
   }

   @Test fun overflowCleanup_finishesBeforeResultIsReturned() = runTest {
      val images = delegate(max = 2)
      val gate = CompletableDeferred<Result<Unit>>()
      storage.beforeDelete = { gate.await() }
      var change: ImageChange? = null
      val job = launch { change = images.importGallery(listOf(uri("a"), uri("b"))).getOrThrow() }
      runCurrent()
      assertNull(change)
      gate.complete(Result.success(Unit))
      job.join()
      assertEquals(listOf(original, "/images/a.jpg"), change!!.paths)
      assertFalse("/images/b.jpg" in storage.files)
      assertEquals(change!!.paths!!.toSet(), storage.files)
   }

   @Test fun duplicateCameraAndOriginalResults_doNotChangeSelectionOrDeleteFiles() = runTest {
      val images = delegate()
      storage.files += photo
      assertEquals(listOf(original, photo), images.acceptCamera(photo).getOrThrow().paths)
      assertEquals(ImageChange(), images.acceptCamera(photo).getOrThrow())
      images.remove(original).getOrThrow()
      assertEquals(ImageChange(), images.acceptCamera(original).getOrThrow())
      images.rejectCamera(photo).getOrThrow()
      images.rejectCamera(original).getOrThrow()
      assertEquals(listOf(photo), session.add(emptyList()))
      assertEquals(setOf(original, photo), storage.files)
   }

   @Test fun rejectFreshCamera_cleansOnceWithoutChangingSession() = runTest {
      val images = delegate()
      storage.files += photo
      images.rejectCamera(photo).getOrThrow()
      images.rejectCamera(photo).getOrThrow()
      assertEquals(ImageChange(), images.acceptCamera(photo).getOrThrow())
      assertEquals(listOf(photo), storage.deleted)
      assertEquals(setOf(original), storage.files)
      assertEquals(listOf(original), session.add(emptyList()))
   }

   @Test fun duplicateRejectedCamera_cannotBeAdoptedDuringPendingCleanup() = runTest {
      val images = delegate()
      storage.files += photo
      val gate = CompletableDeferred<Result<Unit>>()
      storage.beforeDelete = { gate.await() }
      val job = launch { images.rejectCamera(photo).getOrThrow() }
      runCurrent()
      assertEquals(ImageChange(), images.acceptCamera(photo).getOrThrow())
      images.rejectCamera(photo).getOrThrow()
      gate.complete(Result.success(Unit))
      job.join()
      assertEquals(listOf(photo), storage.deleted)
      assertEquals(setOf(original), storage.files)
   }

   @Test fun thrownImportFailure_cleansUnadoptedCopiesBeforeReturningFailure() = runTest {
      storage.copy = {
         if (it == uri("bad")) throw IllegalStateException("provider crashed")
         Result.success(photo)
      }
      val gate = CompletableDeferred<Result<Unit>>()
      storage.beforeDelete = { gate.await() }
      val images = delegate()
      var result: Result<ImageChange>? = null
      val job = launch { result = images.importGallery(listOf(uri("a"), uri("bad"))) }
      runCurrent()
      assertNull(result)
      gate.complete(Result.success(Unit))
      job.join()
      assertTrue(result!!.isFailure)
      assertEquals(setOf(original), storage.files)
      assertEquals(listOf(original), session.add(emptyList()))
   }

   @Test fun cancellation_cleansAllKnownCopiesEvenWhenCallerIsCancelled() = runTest {
      storage.copy = {
         if (it == uri("wait")) awaitCancellation()
         Result.success("/images/${it.lastPathSegment}.jpg")
      }
      val images = delegate()
      val gate = CompletableDeferred<Result<Unit>>()
      storage.beforeDelete = { gate.await() }
      var returned = false
      val job = launch {
         images.importGallery(listOf(uri("a"), uri("b"), uri("wait"), uri("never")))
         returned = true
      }
      runCurrent()
      job.cancel()
      runCurrent()
      assertFalse(job.isCompleted)
      gate.complete(Result.success(Unit))
      job.join()
      assertFalse(returned)
      assertEquals(setOf(original), storage.files)
      assertEquals(listOf("/images/a.jpg", "/images/b.jpg"), storage.deleted)
      assertEquals(listOf(original), session.add(emptyList()))
      assertFalse(uri("never") in storage.copied)
   }

   @Test fun cancellationInResult_isPropagatedAndPriorCopiesAreCleaned() = runTest {
      storage.copy = {
         if (it == uri("cancel")) Result.failure(CancellationException("cancelled"))
         else Result.success(photo)
      }
      val images = delegate()
      try {
         images.importGallery(listOf(uri("a"), uri("cancel")))
         fail("Cancellation must propagate")
      } catch (_: CancellationException) {
         assertEquals(setOf(original), storage.files)
         assertEquals(listOf(original), session.add(emptyList()))
      }
   }

   @Test fun emptyGalleryAndBlankCamera_areUnchanged() = runTest {
      val images = delegate()
      assertEquals(ImageChange(), images.importGallery(emptyList()).getOrThrow())
      assertEquals(ImageChange(), images.acceptCamera(" ").getOrThrow())
      images.rejectCamera("").getOrThrow()
      assertTrue(storage.copied.isEmpty())
      assertTrue(storage.deleted.isEmpty())
   }

   @Test fun discard_restoresOriginalAndDeletesOnlySessionImages() = runTest {
      val images = delegate(ImageSelectionMode.REPLACE, 1)
      storage.files += photo
      images.acceptCamera(photo).getOrThrow()
      images.discard().getOrThrow()
      assertEquals(listOf(original), session.add(emptyList()))
      assertEquals(setOf(original), storage.files)
   }

   @Test fun invalidLimit_isRejectedAtConstruction() {
      for (limit in listOf(0, -1)) {
         try {
            ImageEditDelegate(session, storage, ImageSelectionMode.APPEND, limit)
            fail("Invalid limit")
         } catch (_: IllegalArgumentException) { }
      }
   }

   @Test fun thrownDeleteAfterReplace_doesNotHideChangedSelection() = runTest {
      val images = delegate(ImageSelectionMode.REPLACE, 1)
      storage.files += photo
      images.acceptCamera(photo).getOrThrow()
      storage.beforeDelete = { throw IllegalStateException("delete failed") }
      val change = images.importGallery(listOf(uri("next"))).getOrThrow()
      assertEquals(listOf("/images/next.jpg"), change.paths)
      assertEquals(change.paths, session.add(emptyList()))
      assertTrue(photo in storage.files) // Cleanup failure leaves an orphan, not a stale selection.
   }

   @Test fun thrownDeleteAfterRemove_returnsActualSelection() = runTest {
      val images = delegate()
      storage.files += photo
      images.acceptCamera(photo).getOrThrow()
      storage.beforeDelete = { throw IllegalStateException("delete failed") }
      val change = images.remove(photo).getOrThrow()
      assertEquals(listOf(original), change.paths)
      assertEquals(change.paths, session.add(emptyList()))
   }

   @Test fun cleanupFailure_doesNotSkipOtherUnadoptedFilesOrReplacePrimaryFailure() = runTest {
      storage.copy = {
         if (it == uri("bad")) throw IllegalArgumentException("copy failed")
         Result.success("/images/${it.lastPathSegment}.jpg")
      }
      storage.beforeDelete = {
         if (it == "/images/a.jpg") throw IllegalStateException("delete failed")
         Result.success(Unit)
      }
      val result = delegate().importGallery(listOf(uri("a"), uri("b"), uri("bad")))
      assertTrue(result.exceptionOrNull() is IllegalArgumentException)
      assertEquals(listOf("/images/a.jpg", "/images/b.jpg"), storage.deleted)
      assertFalse("/images/b.jpg" in storage.files)
      assertEquals(listOf(original), session.add(emptyList()))
   }

   @Test fun rejectedCameraCleanupFailure_isReturnedWithoutTouchingOriginal() = runTest {
      val images = delegate()
      storage.files += photo
      storage.beforeDelete = { Result.failure(IllegalStateException("delete failed")) }
      assertTrue(images.rejectCamera(photo).isFailure)
      assertTrue(original in storage.files)
      assertEquals(listOf(original), session.add(emptyList()))
   }

   @Test fun sessionCleanupCancellation_isStillPropagated() = runTest {
      val images = delegate()
      storage.files += photo
      images.acceptCamera(photo).getOrThrow()
      storage.beforeDelete = { throw CancellationException("cancelled") }
      try {
         images.remove(photo)
         fail("Cancellation must propagate")
      } catch (_: CancellationException) { }
   }
}
