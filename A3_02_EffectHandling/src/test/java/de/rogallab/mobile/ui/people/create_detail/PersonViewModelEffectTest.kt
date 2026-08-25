package de.rogallab.mobile.ui.people.create_detail

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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
@Config(sdk = [35])
class PersonViewModelEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   @Test
   fun save_invalidPersonEmitsErrorString() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.FirstNameChange("A"))
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()

         val effect = awaitItem() as PersonEffect.ShowError
         assertTrue(effect.message.isNotEmpty())
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun save_successEmitsMessageStringAndPreparedNavigateBack() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()

         val message = awaitItem() as PersonEffect.ShowMessage
         assertEquals(
            stringProvider.getString(R.string.message_person_saved, "Ada Lovelace"),
            message.message,
         )
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun save_repositoryFailureEmitsErrorWithoutNavigateBack() = runTest(mainDispatcherRule.testDispatcher) {
      repository.createResult = Result.failure(IllegalStateException("write failed"))
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()

         val error = awaitItem() as PersonEffect.ShowError
         assertEquals(stringProvider.getString(R.string.error_person_save), error.message)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun missingExistingPersonEmitsErrorAndStopsLoading() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.success(null)

      val viewModel = PersonViewModel("missing", repository, stringProvider, validator, EffectDelegate())

      viewModel.effects.test {
         advanceUntilIdle()

         val error = awaitItem() as PersonEffect.ShowError
         assertEquals(stringProvider.getString(R.string.error_person_not_found), error.message)
         assertFalse(viewModel.stateFlow.value.isLoading)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
