package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakeImageEdit
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.coroutines.cancellation.CancellationException
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
      source: IPersonRepository = repository,
      imageEdit: IImageEdit = ImageEdit(storage),
      imageStorage: IImageFileStorage = storage,
   ): PersonViewModel {
      return PersonViewModel(
         personId = personId,
         _repository = source,
         _stringProvider = stringProvider,
         _validator = validator,
         _imageFileStorage = imageStorage,
         _imageEdit = imageEdit,
         _personUcCreate = PersonUcCreate(source, imageEdit),
         _personUcUpdate = PersonUcUpdate(source, imageEdit),
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
   fun thrownLoad_setsRetryableFailureAndReleasesLoading() = runTest(mainDispatcherRule.testDispatcher) {
      var throwOnFind = true
      val source = object : IPersonRepository by repository {
         override suspend fun findById(id: String): Result<Person?> {
            if (throwOnFind) throw IllegalStateException("unexpected load failure")
            return Result.success(Person(firstName = "Ada", lastName = "Lovelace", id = id))
         }
      }
      val viewModel = createViewModel("p1", source)
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
      assertFalse(viewModel.stateFlow.value.isLoading)

      throwOnFind = false
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
   }

   @Test
   fun cancelledLoad_releasesLoadingWithoutFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val source = object : IPersonRepository by repository {
         override suspend fun findById(id: String): Result<Person?> =
            throw CancellationException("load cancelled")
      }
      val viewModel = createViewModel("p1", source)
      advanceUntilIdle()
      assertFalse(viewModel.stateFlow.value.isLoading)
      assertNull(viewModel.stateFlow.value.loadFailure)
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
      storage.copyResult = Result.failure(IllegalStateException("copy failed"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
         advanceUntilIdle()
         assertEquals(listOf(sourceUri), storage.copiedUris)
         assertNull(viewModel.stateFlow.value.person.imagePath)
         assertTrue(awaitItem() is PersonEffect.ShowError)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun thrownGalleryCopy_emitsShowErrorAndKeepsState() = runTest(mainDispatcherRule.testDispatcher) {
      val imageStorage = object : IImageFileStorage by storage {
         override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
            throw IllegalStateException("unexpected copy failure")
      }
      val viewModel = createViewModel(imageStorage = imageStorage)

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.GalleryImageSelected(Uri.parse("content://gallery/image/1")))
         advanceUntilIdle()
         assertNull(viewModel.stateFlow.value.person.imagePath)
         assertTrue(awaitItem() is PersonEffect.ShowError)
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
   fun thrownSave_emitsErrorAndReleasesSaving() = runTest(mainDispatcherRule.testDispatcher) {
      val source = object : IPersonRepository by repository {
         override suspend fun create(person: Person): Result<Unit> =
            throw IllegalStateException("unexpected write failure")
      }
      val viewModel = createViewModel(source = source)
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertFalse(viewModel.stateFlow.value.isSaving)
         assertTrue(awaitItem() is PersonEffect.ShowError)
         expectNoEvents()
      }
   }

   @Test
   fun cancelledSave_releasesSavingWithoutError() = runTest(mainDispatcherRule.testDispatcher) {
      val source = object : IPersonRepository by repository {
         override suspend fun create(person: Person): Result<Unit> =
            throw CancellationException("save cancelled")
      }
      val viewModel = createViewModel(source = source)
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
      }
   }

   @Test
   fun failedCancel_staysOnFormForCleanupRetry() = runTest(mainDispatcherRule.testDispatcher) {
      val imageEdit = object : IImageEdit by FakeImageEdit() {
         override suspend fun discard() {
            throw IllegalStateException("cleanup failed")
         }
      }
      val viewModel = createViewModel(imageEdit = imageEdit)

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Cancel)
         advanceUntilIdle()
         assertTrue(awaitItem() is PersonEffect.ShowError)
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
