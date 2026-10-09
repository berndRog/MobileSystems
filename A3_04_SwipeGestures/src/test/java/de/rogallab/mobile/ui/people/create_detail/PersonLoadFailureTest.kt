package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
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
@Config(sdk = [35], application = Application::class)
class PersonLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val strings = FakeStringProvider()
   private val validator = PersonValidator(ApplicationProvider.getApplicationContext())

   @Test
   fun missingPerson_staysVisibleAndCannotBeSaved() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.success(null)
      val viewModel = PersonViewModel("missing", repository, strings, validator, EffectDelegate())
      advanceUntilIdle()

      assertFalse(viewModel.stateFlow.value.isLoading)
      assertEquals(
         strings.getString(R.string.error_person_not_found),
         (viewModel.stateFlow.value.loadFailure as PersonLoadFailure.NotFound).message,
      )
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertTrue(repository.updated.isEmpty())
   }

   @Test
   fun loadFailure_retryLoadsPersonAndClearsError() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.failure(IllegalStateException("offline"))
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())
      advanceUntilIdle()

      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
      assertFalse(viewModel.stateFlow.value.isLoading)

      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      repository.findResult = Result.success(person)
      viewModel.onIntent(PersonIntent.RetryLoad)
      assertEquals(null, viewModel.stateFlow.value.loadFailure)
      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()

      assertEquals(person, viewModel.stateFlow.value.person)
      assertEquals(null, viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }
}
