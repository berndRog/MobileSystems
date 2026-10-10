package de.rogallab.mobile.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
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
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   @Test
   fun cancel_emitsNavigateBackWithCancelReason() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(
         personId = null,
         _repository = FakePersonRepository(),
         _stringProvider = stringProvider,
         _validator = validator,
         _effectDelegate = EffectDelegate(),
      )

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
         val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Save)
            viewModel.onIntent(PersonIntent.Cancel)
            assertTrue(viewModel.stateFlow.value.isSaving)
            runCurrent()
            assertEquals(1, createCalls)
            expectNoEvents()

            pending.complete(Result.success(Unit))
            advanceUntilIdle()

            assertTrue(awaitItem() is PersonEffect.ShowMessage)
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
         val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

         viewModel.effects.test {
            viewModel.onIntent(PersonIntent.Save)
            runCurrent()
            assertTrue(viewModel.stateFlow.value.isSaving)

            pending.complete(Result.failure(IllegalStateException("offline")))
            advanceUntilIdle()

            assertTrue(awaitItem() is PersonEffect.ShowError)
            assertFalse(viewModel.stateFlow.value.isSaving)
            expectNoEvents()

            viewModel.onIntent(PersonIntent.Cancel)
            advanceUntilIdle()
            assertEquals(BackReason.Cancel, (awaitItem() as PersonEffect.NavigateBack).reason)
            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun thrownSaveException_emitsErrorAndResetsSavingWithoutNavigation() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun create(person: Person): Result<Unit> {
               throw IllegalStateException("unexpected repository failure")
            }
         }
         val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
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
