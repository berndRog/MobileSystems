package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import android.util.Log
import de.rogallab.mobile.shared.domain.utilities.Alog
import org.robolectric.shadows.ShadowLog
import de.rogallab.mobile.shared.R as SharedR
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageChange
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class PersonViewModelImageTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val storage = FakeImageFileStorage()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   private fun createViewModel(
      personId: String? = null,
      imageStorage: IImageFileStorage = storage,
      images: IImageEditDelegate? = null,
   ): PersonViewModel {
      val imageEdit = ImageEdit(imageStorage)

      return PersonViewModel(
         personId = personId,
         _repository = repository,
         _stringProvider = stringProvider,
         _validator = validator,
         _images = images ?: ImageEditDelegate(imageEdit, imageStorage, ImageSelectionMode.REPLACE, 1),
         _personUcCreate = PersonUcCreate(repository, imageEdit),
         _personUcUpdate = PersonUcUpdate(repository, imageEdit),
         _timing = AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10),
         _effectDelegate = EffectDelegate(),
      )
   }

   @Test
   fun failedLoad_staysVisibleUntilRetry() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.failure(IllegalStateException("offline"))
      val viewModel = createViewModel("p1")
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
      assertFalse(viewModel.stateFlow.value.isLoading)

      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")
      repository.findResult = Result.success(person)
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertEquals(person, viewModel.stateFlow.value.person)
   }

   @Test
   fun missingPerson_showsPersistentNotFound() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel("missing")
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
   }

   @Test
   fun galleryImageSelected_copiesImageAndUpdatesState() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val sourceUri = Uri.parse("content://gallery/image/1")

      viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
      advanceUntilIdle()

      assertEquals(listOf(sourceUri), storage.copiedUris)
      assertEquals("/images/copied.jpg", viewModel.stateFlow.value.person.imagePath)
   }

   @Test
   fun galleryCopyFailure_emitsShowErrorAndKeepsState() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val sourceUri = Uri.parse("content://gallery/image/1")
      Alog.set(useAndroidLog = true)
      ShadowLog.clear()
      storage.copyResult = Result.failure(IllegalStateException("copy failed"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
         advanceUntilIdle()
         assertEquals(listOf(sourceUri), storage.copiedUris)
         assertNull(viewModel.stateFlow.value.person.imagePath)
         assertEquals(PersonEffect.ShowError(stringProvider.getString(SharedR.string.error_image_save)), awaitItem())
         assertEquals(1, ShadowLog.getLogsForTag("<-PersonViewModel").count { it.type == Log.ERROR })
         assertFalse(viewModel.stateFlow.value.isEditingImages)
         expectNoEvents()
      }
   }

   @Test
   fun imagePathChange_updatesState() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()
      assertEquals("/images/new.jpg", viewModel.stateFlow.value.person.imagePath)
   }

   @Test
   fun replacingUnsavedImage_deletesPreviousReplacement() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/one.jpg"))
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/two.jpg"))
      advanceUntilIdle()
      assertTrue("/images/one.jpg" in storage.deletedPaths)
      assertEquals("/images/two.jpg", viewModel.stateFlow.value.person.imagePath)
   }

   @Test
   fun cancel_deletesUnsavedReplacementAndEmitsNavigateBack() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Cancel)
         advanceUntilIdle()
         assertTrue("/images/new.jpg" in storage.deletedPaths)
         val effect = awaitItem() as PersonEffect.NavigateBack
         assertEquals(BackReason.Cancel, effect.reason)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun saveReplacement_deletesPersistedOriginalOnlyAfterSuccessfulSave() = runTest(mainDispatcherRule.testDispatcher) {
      val original = Person(firstName = "Ada",lastName = "Lovelace",imagePath = "/images/original.jpg",id = "p1")
      repository.findResult = Result.success(original)
      val viewModel = createViewModel("p1")
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()
      assertFalse("/images/original.jpg" in storage.deletedPaths)
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertTrue("/images/original.jpg" in storage.deletedPaths)
      assertEquals(1, repository.updated.size)
   }

   @Test
   fun failedSave_keepsPersistedOriginalAndReplacementPending() = runTest(mainDispatcherRule.testDispatcher) {
      val original = Person(firstName = "Ada",lastName = "Lovelace",imagePath = "/images/original.jpg",id = "p1")
      repository.findResult = Result.success(original)
      repository.updateResult = Result.failure(IllegalStateException("write failed"))
      val viewModel = createViewModel("p1")
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertFalse("/images/original.jpg" in storage.deletedPaths)
      assertFalse("/images/new.jpg" in storage.deletedPaths)
      assertEquals("/images/new.jpg", viewModel.stateFlow.value.person.imagePath)
      viewModel.onIntent(PersonIntent.Cancel)
      advanceUntilIdle()
      assertTrue("/images/new.jpg" in storage.deletedPaths)
   }

   @Test
   fun galleryImport_blocksSaveCancelAndFurtherImages_untilFinished() =
      runTest(mainDispatcherRule.testDispatcher) {
         val pending = CompletableDeferred<Result<String>>()
         val delayedStorage = object : IImageFileStorage by storage {
            override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
               pending.await()
         }
         val viewModel = createViewModel(imageStorage = delayedStorage)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
            assertTrue(viewModel.stateFlow.value.isEditingImages)
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Cancel)
            viewModel.onIntent(PersonIntent.CameraImageTaken("/images/rejected.jpg"))
            advanceUntilIdle()
            assertTrue(repository.created.isEmpty())
            assertTrue("/images/rejected.jpg" in storage.deletedPaths)
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()

            pending.complete(Result.success("/images/imported.jpg"))
            advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isEditingImages)
            assertEquals("/images/imported.jpg", viewModel.stateFlow.value.person.imagePath)
            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(BackReason.Cancel, (awaitItem() as PersonEffect.NavigateBack).reason)
            assertTrue("/images/imported.jpg" in storage.deletedPaths)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun invalidSave_showsNormalizedValuesWithoutStartingWrite() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel()
         viewModel.onIntent(PersonIntent.FirstNameChange("   "))
         viewModel.onIntent(PersonIntent.LastNameChange("  Lovelace  "))
         viewModel.onIntent(PersonIntent.EmailChange(" müller@example.de! "))
         viewModel.onIntent(PersonIntent.PhoneChange(" +49abc123456 "))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)

            val person = viewModel.stateFlow.value.person
            assertEquals("", person.firstName)
            assertEquals("Lovelace", person.lastName)
            assertEquals("mueller@example.de", person.email)
            assertEquals("+49123456", person.phone)
            assertFalse(viewModel.stateFlow.value.isSaving)
            advanceUntilIdle()
            assertTrue(repository.created.isEmpty())
            assertTrue(repository.updated.isEmpty())
            assertTrue(awaitItem() is PersonEffect.ShowError)
            expectNoEvents()
         }
      }

   @Test
   fun validSave_persistsTheNormalizedSnapshotShownInUi() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel()
         viewModel.onIntent(PersonIntent.FirstNameChange("  Ada  "))
         viewModel.onIntent(PersonIntent.LastNameChange("  Lovelace  "))
         viewModel.onIntent(PersonIntent.EmailChange(" müller@example.de! "))
         viewModel.onIntent(PersonIntent.PhoneChange(" +49abc123456 "))

         viewModel.onIntent(PersonIntent.Save)
         val snapshot = viewModel.stateFlow.value.person
         assertEquals("Ada", snapshot.firstName)
         assertEquals("Lovelace", snapshot.lastName)
         assertEquals("mueller@example.de", snapshot.email)
         assertEquals("+49123456", snapshot.phone)
         assertTrue(viewModel.stateFlow.value.isSaving)

         advanceUntilIdle()
         assertEquals(snapshot, repository.created.single())
      }

   @Test
   fun save_blocksFormAndImageChanges_andCleansRejectedCamera() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel()
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
         viewModel.onIntent(PersonIntent.Save)
         viewModel.onIntent(PersonIntent.FirstNameChange("Grace"))
         viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
         viewModel.onIntent(PersonIntent.CameraImageTaken("/images/rejected.jpg"))
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertEquals("Ada", repository.created.single().firstName)
         assertTrue(storage.copiedUris.isEmpty())
         assertTrue("/images/rejected.jpg" in storage.deletedPaths)
         assertNull(viewModel.stateFlow.value.person.imagePath)
      }

   @Test
   fun cancelledImport_resetsEditingFlag_withoutErrorEffect() =
      runTest(mainDispatcherRule.testDispatcher) {
         val cancelledStorage = object : IImageFileStorage by storage {
            override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
               throw CancellationException("cancelled import")
         }
         val viewModel = createViewModel(imageStorage = cancelledStorage)
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
            advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isEditingImages)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun cancel_blocksFurtherActions_andCleansLateCameraResult() =
      runTest(mainDispatcherRule.testDispatcher) {
         val viewModel = createViewModel()
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Cancel)
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Cancel)
            viewModel.onIntent(PersonIntent.CameraImageTaken("/images/late.jpg"))
            advanceUntilIdle()
            assertTrue(repository.created.isEmpty())
            assertTrue("/images/late.jpg" in storage.deletedPaths)
            assertNull(viewModel.stateFlow.value.person.imagePath)
            assertEquals(BackReason.Cancel, (awaitItem() as PersonEffect.NavigateBack).reason)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun completedImageJob_doesNotReleaseTheNextPendingChange() =
      runTest(mainDispatcherRule.testDispatcher) {
         val pending = CompletableDeferred<Result<String>>()
         val delayedStorage = object : IImageFileStorage by storage {
            override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
               pending.await()
         }
         val viewModel = createViewModel(imageStorage = delayedStorage)
         viewModel.onIntent(PersonIntent.CameraImageTaken("/images/first.jpg"))
         // Camera acceptance completes before its completion handler is registered.
         assertFalse(viewModel.stateFlow.value.isEditingImages)
         assertEquals("/images/first.jpg", viewModel.stateFlow.value.person.imagePath)

         viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
         advanceUntilIdle()
         assertTrue(viewModel.stateFlow.value.isEditingImages)
         viewModel.onIntent(PersonIntent.CameraImageTaken("/images/rejected.jpg"))
         assertTrue(viewModel.stateFlow.value.isEditingImages)
         assertTrue("/images/rejected.jpg" in storage.deletedPaths)

         pending.complete(Result.success("/images/second.jpg"))
         advanceUntilIdle()
         assertFalse(viewModel.stateFlow.value.isEditingImages)
         assertEquals("/images/second.jpg", viewModel.stateFlow.value.person.imagePath)
      }

   @Test
   fun scopeCancellation_resetsImageGuard_evenForAnAlreadyCancelledScope() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (cancelBeforeIntent in listOf(false, true)) {
            var copies = 0
            val pending = CompletableDeferred<Result<String>>()
            val delayedStorage = object : IImageFileStorage by storage {
               override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> {
                  copies++
                  return pending.await()
               }
            }
            val store = ViewModelStore()
            val viewModel = createViewModel(imageStorage = delayedStorage)
               .also { store.put("person", it) }
            viewModel.effects.test {
               if (cancelBeforeIntent) store.clear()
               viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
               if (!cancelBeforeIntent) {
                  assertTrue(viewModel.stateFlow.value.isEditingImages)
                  store.clear()
                  // Cancellation must finish before the completion handler releases the guard.
                  runCurrent()
               }
               assertFalse(viewModel.stateFlow.value.isEditingImages)
               advanceUntilIdle()
               assertEquals(if (cancelBeforeIntent) 0 else 1, copies)
               assertNull(viewModel.stateFlow.value.person.imagePath)
               expectNoEvents()
            }
         }
      }

   @Test
   fun cancellationResultFromDelegate_cancelsImageJob_andReleasesGuard() =
      runTest(mainDispatcherRule.testDispatcher) {
         val pending = CompletableDeferred<Result<ImageChange>>()
         val delegate = ImageEditDelegate(ImageEdit(storage), storage, ImageSelectionMode.REPLACE, 1)
         val images = object : IImageEditDelegate by delegate {
            override suspend fun importGallery(uris: List<Uri>): Result<ImageChange> =
               pending.await()
         }
         val store = ViewModelStore()
         val viewModel = createViewModel(images = images).also { store.put("person", it) }
         try {
            viewModel.effects.test {
               viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
               val imageJob = viewModel.viewModelScope.coroutineContext[
                  kotlinx.coroutines.Job
               ]!!.children.single()
               pending.complete(Result.failure(CancellationException("cancelled import")))
               advanceUntilIdle()
               assertTrue(imageJob.isCancelled)
               assertFalse(viewModel.stateFlow.value.isEditingImages)
               assertNull(viewModel.stateFlow.value.person.imagePath)
               expectNoEvents()
            }
         } finally {
            store.clear()
         }
      }

   @Test
   fun failedDiscard_keepsTheImageAndAllowsAnotherCancelAttempt() =
      runTest(mainDispatcherRule.testDispatcher) {
         val delegate = ImageEditDelegate(ImageEdit(storage), storage, ImageSelectionMode.REPLACE, 1)
         var discards = 0
         val images = object : IImageEditDelegate by delegate {
            override suspend fun discard(): Result<Unit> {
               discards++
               return if (discards == 1) Result.failure(IllegalStateException("discard failed"))
               else delegate.discard()
            }
         }
         val viewModel = createViewModel(images = images)
         viewModel.onIntent(PersonIntent.CameraImageTaken("/images/new.jpg"))
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertTrue(awaitItem() is PersonEffect.ShowError)
            assertFalse("/images/new.jpg" in storage.deletedPaths)
            expectNoEvents()

            viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
            assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(BackReason.Cancel, (awaitItem() as PersonEffect.NavigateBack).reason)
            assertEquals(listOf("/images/new.jpg"), storage.deletedPaths)
            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(2, discards)
            expectNoEvents()
         }
      }

   @Test
   fun cancelledDiscard_propagatesWithoutErrorOrNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val delegate = ImageEditDelegate(ImageEdit(storage), storage, ImageSelectionMode.REPLACE, 1)
            val images = object : IImageEditDelegate by delegate {
               override suspend fun discard(): Result<Unit> {
                  val cancellation = CancellationException("cancelled discard")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val store = ViewModelStore()
            val viewModel = createViewModel(images = images).also { store.put("person", it) }
            try {
               viewModel.effects.test {
                  viewModel.onIntent(PersonIntent.Cancel)
                  val discardJob = viewModel.viewModelScope.coroutineContext[
                     kotlinx.coroutines.Job
                  ]!!.children.single()
                  advanceUntilIdle()
                  assertTrue(discardJob.isCancelled)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun partialImageChange_emitsOneErrorAndKeepsTheUpdatedSelection() =
      runTest(mainDispatcherRule.testDispatcher) {
         val delegate = ImageEditDelegate(ImageEdit(storage), storage, ImageSelectionMode.REPLACE, 1)
         val images = object : IImageEditDelegate by delegate {
            override suspend fun importGallery(uris: List<Uri>): Result<ImageChange> =
               Result.success(ImageChange(paths = listOf("/images/partial.jpg"), failedImports = 1))
         }
         val viewModel = createViewModel(images = images)
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
            advanceUntilIdle()
            assertEquals("/images/partial.jpg", viewModel.stateFlow.value.person.imagePath)
            assertFalse(viewModel.stateFlow.value.isEditingImages)
            assertEquals(PersonEffect.ShowError(stringProvider.getString(SharedR.string.error_image_save)), awaitItem())
            expectNoEvents()
         }
      }

   @Test
   fun rejectedCameraCleanupFailure_isLoggedOnlyByShared_withoutReleasingTheImageGuard() =
      runTest(mainDispatcherRule.testDispatcher) {
         Alog.set(useAndroidLog = true)
         ShadowLog.clear()
         val pending = CompletableDeferred<Result<String>>()
         val delayedStorage = object : IImageFileStorage by storage {
            override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> = pending.await()
         }
         storage.deleteResult = Result.failure(IllegalStateException("cleanup failed"))
         val viewModel = createViewModel(imageStorage = delayedStorage)
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/1")))
            viewModel.onIntent(PersonIntent.CameraImageTaken("/images/rejected.jpg"))
            advanceUntilIdle()
            assertEquals(PersonEffect.ShowError(stringProvider.getString(SharedR.string.error_image_save)), awaitItem())
            assertTrue(viewModel.stateFlow.value.isEditingImages)
            assertEquals(listOf("/images/rejected.jpg"), storage.deletedPaths)
            assertEquals(1, ShadowLog.getLogsForTag("<-ImageEditDelegate").count { it.type == Log.ERROR })
            assertTrue(ShadowLog.getLogsForTag("<-PersonViewModel").none { it.type == Log.ERROR })
            expectNoEvents()

            pending.complete(Result.success("/images/selected.jpg"))
            advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isEditingImages)
            assertEquals("/images/selected.jpg", viewModel.stateFlow.value.person.imagePath)
            expectNoEvents()
         }
      }

   @Test
   fun imageStorageFailed_isForwardedAsShowErrorString() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.ImageFailed("camera failed"))
         advanceUntilIdle()
         val effect = awaitItem() as PersonEffect.ShowError
         assertEquals("camera failed", effect.message)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
