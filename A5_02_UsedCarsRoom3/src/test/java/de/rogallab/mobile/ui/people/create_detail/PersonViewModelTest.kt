package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarRepository
import de.rogallab.mobile.testing.FakeImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
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
@Config(sdk = [35], application = Application::class)
class PersonViewModelTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val repository = FakePersonRepository()
   private val imageEdit = FakeImageEdit()
   private val storage = FakeImageFileStorage()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

   private fun viewModel(id: String? = null) = PersonViewModel(
      personId = id,
      _repository = repository,
      _carRepository = FakeCarRepository(),
      _stringProvider = FakeStringProvider(),
      _validator = PersonValidator(context),
      _imageFileStorage = storage,
      _imageEdit = imageEdit,
      _personUcCreate = PersonUcCreate(repository, imageEdit),
      _personUcUpdate = PersonUcUpdate(repository, imageEdit),
      _effectDelegate = EffectDelegate(),
   )

   @Test fun failedLoad_staysVisibleUntilRetry() = runTest(dispatcherRule.testDispatcher) {
      repository.findResult = Result.failure(IllegalStateException("offline"))
      val vm = viewModel("p1")
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
      assertFalse(vm.stateFlow.value.isLoading)
      val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      repository.findResult = Result.success(person)
      vm.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(person, vm.stateFlow.value.person)
   }

   @Test fun missingPerson_hasPersistentNotFound() = runTest(dispatcherRule.testDispatcher) {
      val vm = viewModel("missing")
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
      vm.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
   }

   @Test fun successfulSave_usesUseCaseThenNavigatesBack() = runTest(dispatcherRule.testDispatcher) {
      val vm = viewModel()
      vm.onIntent(PersonIntent.FirstNameChange("Ada"))
      vm.onIntent(PersonIntent.LastNameChange("Lovelace"))
      vm.effects.test {
         vm.onIntent(PersonIntent.Save)
         advanceUntilIdle()
         assertEquals(1, repository.created.size)
         assertEquals(1, imageEdit.commitCount)
         assertTrue(awaitItem() is PersonEffect.ShowMessage)
         assertEquals(BackReason.Save, (awaitItem() as PersonEffect.NavigateBack).reason)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test fun failedSave_keepsEditorAndImageSession() = runTest(dispatcherRule.testDispatcher) {
      repository.createResult = Result.failure(IllegalStateException("write failed"))
      val vm = viewModel()
      vm.onIntent(PersonIntent.FirstNameChange("Ada"))
      vm.onIntent(PersonIntent.LastNameChange("Lovelace"))
      vm.effects.test {
         vm.onIntent(PersonIntent.Save)
         advanceUntilIdle()
         assertTrue(awaitItem() is PersonEffect.ShowError)
         assertTrue(repository.created.isEmpty())
         assertEquals(0, imageEdit.commitCount)
         assertFalse(vm.stateFlow.value.isLoading)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
