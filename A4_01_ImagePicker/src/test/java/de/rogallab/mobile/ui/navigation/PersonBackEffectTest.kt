package de.rogallab.mobile.ui.navigation

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
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
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class PersonBackEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val timing = AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10)
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   private fun createViewModel(repository: IPersonRepository = FakePersonRepository()): PersonViewModel {
      val imageFileStorage = FakeImageFileStorage()
      val imageEdit = ImageEdit(imageFileStorage)
      return PersonViewModel(
         personId = null,
         _repository = repository,
         _stringProvider = stringProvider,
         _validator = validator,
         _images = ImageEditDelegate(imageEdit, imageFileStorage, ImageSelectionMode.REPLACE, 1),
         _personUcCreate = PersonUcCreate(repository, imageEdit),
         _personUcUpdate = PersonUcUpdate(repository, imageEdit),
         _timing = timing,
         _effectDelegate = EffectDelegate(),
      )
   }

   @Test
   fun cancelledSave_resetsSaving_withoutErrorOrNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override suspend fun create(person: Person): Result<Unit> {
                  val cancellation = kotlinx.coroutines.CancellationException("cancelled write")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = createViewModel(repository)
            viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
            viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
            viewModel.effects.test {
               viewModel.onIntent(PersonIntent.Save)
               advanceUntilIdle()
               assertFalse(viewModel.stateFlow.value.isSaving)
               expectNoEvents()
               cancelAndIgnoreRemainingEvents()
            }
         }
      }

   @Test
   fun scopeCancellation_resetsSaving_evenBeforeWorkStarts() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (cancelBeforeIntent in listOf(false, true)) {
            val repository = FakePersonRepository()
            val store = ViewModelStore()
            val viewModel = createViewModel(repository).also { store.put("person", it) }
            viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
            viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

            viewModel.effects.test {
               if (cancelBeforeIntent) store.clear()
               viewModel.onIntent(PersonIntent.Save)
               if (!cancelBeforeIntent) {
                  assertTrue(viewModel.stateFlow.value.isSaving)
                  store.clear()
                  // Complete the cancelled continuation before checking its completion handler.
                  runCurrent()
               }
               // Registration must also work when the job has already been cancelled.
               assertFalse(viewModel.stateFlow.value.isSaving)
               advanceUntilIdle()
               assertTrue(repository.created.isEmpty())
               expectNoEvents()
            }
         }
      }

   @Test
   fun failedSave_releasesGuardForOneRetry_andKeepsItUntilRetryCompletes() =
      runTest(mainDispatcherRule.testDispatcher) {
         val source = FakePersonRepository()
         val pending = CompletableDeferred<Result<Unit>>()
         var calls = 0
         val repository = object : IPersonRepository by source {
            override suspend fun create(person: Person): Result<Unit> {
               calls++
               return if (calls == 1) Result.failure(IllegalStateException("offline"))
               else pending.await().onSuccess { source.create(person) }
            }
         }
         val viewModel = createViewModel(repository)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            advanceUntilIdle()
            assertTrue(awaitItem() is PersonEffect.ShowError)
            assertFalse(viewModel.stateFlow.value.isSaving)

            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(2, calls)
            assertTrue(viewModel.stateFlow.value.isSaving)
            expectNoEvents()

            pending.complete(Result.success(Unit))
            advanceUntilIdle()
            assertFalse(viewModel.stateFlow.value.isSaving)
            assertEquals(1, source.created.size)
            assertTrue(awaitItem() is PersonEffect.ShowMessage)
            assertEquals(BackReason.Save, (awaitItem() as PersonEffect.NavigateBack).reason)
            expectNoEvents()
         }
      }

   @Test
   fun invalidPerson_emitsValidationErrorWithoutWritingOrNavigating() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository()
         val viewModel = createViewModel(repository)

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.FirstNameChange("A"))
            viewModel.onIntent(PersonIntent.Save)
            advanceUntilIdle()

            val effect = awaitItem() as PersonEffect.ShowError
            assertTrue(effect.message.isNotEmpty())
            assertTrue(repository.created.isEmpty())
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun cancel_emitsNavigateBackWithCancelReason() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Cancel)
         advanceUntilIdle()

         val effect = awaitItem() as PersonEffect.NavigateBack
         assertEquals(BackReason.Cancel, effect.reason)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun save_blocksDuplicateSaveAndCancel_thenEmitsMessageBeforeNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         val pending = CompletableDeferred<Result<Unit>>()
         var createCalls = 0
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun create(person: Person): Result<Unit> {
               createCalls++
               return pending.await()
            }
         }
         val viewModel = createViewModel(repository)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Cancel)
            assertTrue(viewModel.stateFlow.value.isSaving)
            advanceTimeBy(timing.delayWrite)
            runCurrent()
            assertEquals(1, createCalls)
            expectNoEvents()

            pending.complete(Result.success(Unit))
            advanceUntilIdle()

            val message = awaitItem() as PersonEffect.ShowMessage
            assertEquals(
               stringProvider.getString(R.string.message_person_saved, "Ada Lovelace"),
               message.message,
            )
            assertEquals(BackReason.Save, (awaitItem() as PersonEffect.NavigateBack).reason)
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun failedSave_emitsErrorAndAllowsCancelAfterwards() =
      runTest(mainDispatcherRule.testDispatcher) {
         val pending = CompletableDeferred<Result<Unit>>()
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun create(person: Person): Result<Unit> = pending.await()
         }
         val viewModel = createViewModel(repository)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            runCurrent()
            assertTrue(viewModel.stateFlow.value.isSaving)

            pending.complete(Result.failure(IllegalStateException("offline")))
            advanceUntilIdle()

            val error = awaitItem() as PersonEffect.ShowError
            assertEquals(stringProvider.getString(R.string.error_person_save), error.message)
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()

            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(BackReason.Cancel, (awaitItem() as PersonEffect.NavigateBack).reason)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun failedSaveResult_emitsErrorAndResetsSavingWithoutNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository().apply {
            createResult = Result.failure(IllegalStateException("repository failure"))
         }
         val viewModel = createViewModel(repository)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            assertTrue(viewModel.stateFlow.value.isSaving)
            advanceUntilIdle()

            assertTrue(awaitItem() is PersonEffect.ShowError)
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
         }
      }
}
