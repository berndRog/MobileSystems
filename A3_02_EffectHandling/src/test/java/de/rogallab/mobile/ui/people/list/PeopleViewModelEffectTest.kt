package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun successfulObservationKeepsUpdatingPeople() = runTest(mainDispatcherRule.testDispatcher) {
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val grace = Person(firstName = "Grace", lastName = "Hopper", id = "person-2")
      val repository = FakePersonRepository(listOf(ada))
      val stringProvider = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, stringProvider, EffectDelegate())
      advanceUntilIdle()

      assertEquals(listOf(ada), viewModel.stateFlow.value.people)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)

      repository.peopleFlow.value = Result.success(listOf(ada, grace))
      advanceUntilIdle()
      assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
   }

   @Test
   fun emptyListIsSuccessfulObservation() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PeopleViewModel(FakePersonRepository(), FakeStringProvider(), EffectDelegate())
      advanceUntilIdle()

      assertTrue(viewModel.stateFlow.value.people.isEmpty())
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun observeFailureStaysVisibleUntilRetrySucceeds() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("read failed"))
      }
      var observations = 0
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> {
            observations++
            return source.peopleFlow
         }
      }
      val stringProvider = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, stringProvider, EffectDelegate())
      advanceUntilIdle()

      assertEquals(stringProvider.getString(R.string.error_people_observe),
         viewModel.stateFlow.value.loadFailure)
      assertEquals(1, observations)
      assertFalse(viewModel.stateFlow.value.isLoading)

      // A later repository value must not clear the error before RetryLoad.
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      source.peopleFlow.value = Result.success(listOf(ada))
      advanceUntilIdle()
      assertEquals(stringProvider.getString(R.string.error_people_observe),
         viewModel.stateFlow.value.loadFailure)

      viewModel.onIntent(PeopleIntent.RetryLoad)
      viewModel.onIntent(PeopleIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()

      assertEquals(2, observations)
      assertEquals(listOf(ada), viewModel.stateFlow.value.people)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun thrownObservationExceptionBecomesPersistentFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository()
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> = flow {
            throw IllegalStateException("unexpected")
         }
      }
      val stringProvider = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, stringProvider, EffectDelegate())
      advanceUntilIdle()

      assertEquals(stringProvider.getString(R.string.error_people_observe),
         viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Erfolgreiche Room-Emissionen aktualisieren den State fortlaufend.
 * - Nach einem Ladefehler startet erst RetryLoad einen neuen Collector.
 */
