package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun failedObservation_staysVisibleAndRetryRestartsObservation() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("offline"))
      }
      val strings = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())
      advanceUntilIdle()

      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadError)
      assertFalse(viewModel.stateFlow.value.isLoading)
      assertEquals(1, repository.observeCalls)

      viewModel.onIntent(PeopleIntent.RetryLoad)
      assertEquals(null, viewModel.stateFlow.value.loadError)
      assertEquals(true, viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()
      assertEquals(2, repository.observeCalls)
      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadError)

      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      repository.peopleFlow.value = Result.success(listOf(person))
      advanceUntilIdle()
      assertEquals(null, viewModel.stateFlow.value.loadError)
      assertEquals(listOf(person), viewModel.stateFlow.value.people)
   }
}
