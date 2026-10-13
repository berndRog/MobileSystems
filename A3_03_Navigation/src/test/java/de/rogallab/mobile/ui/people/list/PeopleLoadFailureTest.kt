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
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val strings = FakeStringProvider()

   @Test
   fun successfulLoad_keepsObservingLaterPeopleUpdates() = runTest(mainDispatcherRule.testDispatcher) {
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val grace = Person(firstName = "Grace", lastName = "Hopper", id = "person-2")
      val repository = FakePersonRepository(listOf(ada))
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())

      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()
      assertEquals(listOf(ada), viewModel.stateFlow.value.people)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)

      repository.peopleFlow.value = Result.success(listOf(ada, grace))
      advanceUntilIdle()
      assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
   }

   @Test
   fun emptyList_isASuccessfulLoad() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PeopleViewModel(FakePersonRepository(), strings, EffectDelegate())
      advanceUntilIdle()

      assertTrue(viewModel.stateFlow.value.people.isEmpty())
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun failedResult_staysVisibleUntilRetrySucceeds() = runTest(mainDispatcherRule.testDispatcher) {
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val source = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("offline"))
      }
      var observations = 0
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> {
            observations++
            return source.peopleFlow
         }
      }
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())
      advanceUntilIdle()

      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
      assertEquals(1, observations)

      // A new repository value must not silently clear a failed attempt.
      source.peopleFlow.value = Result.success(listOf(ada))
      advanceUntilIdle()
      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadFailure)
      assertTrue(viewModel.stateFlow.value.people.isEmpty())

      viewModel.onIntent(PeopleIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      assertNull(viewModel.stateFlow.value.loadFailure)
      viewModel.onIntent(PeopleIntent.RetryLoad)
      advanceUntilIdle()

      assertEquals(2, observations)
      assertEquals(listOf(ada), viewModel.stateFlow.value.people)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun thrownRepositoryException_becomesPersistentFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository()
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> = flow {
            throw IllegalStateException("unexpected load failure")
         }
      }
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())
      advanceUntilIdle()

      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun retryDuringInitialLoading_doesNotStartAnotherObservation() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository()
      var observations = 0
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> {
            observations++
            return source.peopleFlow
         }
      }
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())

      viewModel.onIntent(PeopleIntent.RetryLoad)
      viewModel.onIntent(PeopleIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()

      assertEquals(1, observations)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun cancellationResult_isNotShownAsLoadFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository()
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> = flow {
            emit(Result.failure(CancellationException("cancelled")))
         }
      }
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())
      advanceUntilIdle()

      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Tests unterscheiden einen gültigen leeren Datenbestand von einem Ladefehler.
 * - Ein fehlgeschlagener Flow bleibt beendet, bis RetryLoad ihn neu beobachtet.
 * - Laufende Versuche und CancellationException erzeugen keinen zweiten Ladefehler.
 */
