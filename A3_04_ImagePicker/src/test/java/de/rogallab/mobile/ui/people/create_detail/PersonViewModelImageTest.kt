package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@Config(sdk = [35])
class PersonViewModelImageTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val storage = FakeImageFileStorage()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   private fun createViewModel(personId: String? = null) =
      PersonViewModel(
         personId = personId,
         _repository = repository,
         _stringProvider = stringProvider,
         _validator = validator,
         _imageFileStorage = storage,
         _imageEdit = ImageEditDelegate(storage),
         _effectDelegate = EffectDelegate(),
      )

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
   fun imagePathChange_updatesState() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/new.jpg"))
      advanceUntilIdle()
      assertEquals("/images/new.jpg", viewModel.stateFlow.value.person.imagePath)
   }

   @Test
   fun replacingUnsavedImage_deletesPreviousReplacement() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/one.jpg"))
      advanceUntilIdle()
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/two.jpg"))
      advanceUntilIdle()
      assertTrue("/images/one.jpg" in storage.deletedPaths)
      assertEquals("/images/two.jpg", viewModel.stateFlow.value.person.imagePath)
   }

   @Test
   fun cancel_deletesUnsavedReplacementAndEmitsNavigateBack() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/new.jpg"))
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
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/new.jpg"))
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
      viewModel.onIntent(PersonIntent.ImagePathChange("/images/new.jpg"))
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
   fun imageStorageFailed_isForwardedAsShowErrorString() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.ImageStorageFailed("camera failed"))
         advanceUntilIdle()
         val effect = awaitItem() as PersonEffect.ShowError
         assertEquals("camera failed", effect.message)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
