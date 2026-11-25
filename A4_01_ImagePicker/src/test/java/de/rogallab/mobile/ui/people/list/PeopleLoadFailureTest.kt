package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val strings = FakeStringProvider()

   private fun createViewModel(repository: IPersonRepository) = PeopleViewModel(
      _repository = repository,
      _personUcDelete = PersonUcDelete(repository, FakeImageFileStorage()),
      _stringProvider = strings,
      _effectDelegate = EffectDelegate(),
      _timing = AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10),
   )

   @Test
   fun successfulLoad_keepsObservingLaterPeopleUpdates() = runTest(mainDispatcherRule.testDispatcher) {
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val grace = Person(firstName = "Grace", lastName = "Hopper", id = "person-2")
      val repository = FakePersonRepository(listOf(ada))
      val viewModel = createViewModel(repository)

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
      val viewModel = createViewModel(FakePersonRepository())
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
            return source.observeAll()
         }
      }
      val viewModel = createViewModel(repository)
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
   fun repositoryFailure_becomesPersistentFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("load failure"))
      }
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      assertEquals(strings.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun retry_waitsForCollectorCleanup_andIgnoresFollowingEmissions() =
      runTest(mainDispatcherRule.testDispatcher) {
         val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
         var collections = 0
         var active = 0
         var maxActive = 0
         var continuedAfterFailure = false
         val repository = object : IPersonRepository by FakePersonRepository() {
            override fun observeAll(): Flow<Result<List<Person>>> = flow {
               val attempt = ++collections
               active++
               maxActive = maxOf(maxActive, active)
               try {
                  if (attempt == 1) {
                     emit(Result.failure(IllegalStateException("offline")))
                     continuedAfterFailure = true
                     emit(Result.success(listOf(ada)))
                  } else {
                     emit(Result.success(listOf(ada)))
                     awaitCancellation()
                  }
               } finally {
                  // Simulate slow upstream cleanup after collector cancellation.
                  if (attempt == 1) withContext(NonCancellable) { delay(100) }
                  active--
               }
            }
         }
         val store = ViewModelStore()
         val viewModel = createViewModel(repository).also { store.put("people", it) }
         try {
            advanceTimeBy(10)
            runCurrent()
            assertFalse(viewModel.stateFlow.value.isLoading)
            assertTrue(viewModel.stateFlow.value.loadFailure != null)
            assertTrue(viewModel.stateFlow.value.people.isEmpty())
            assertFalse(continuedAfterFailure)
            assertEquals(1, active)

            repeat(10) { viewModel.onIntent(PeopleIntent.RetryLoad) }
            runCurrent()
            assertTrue(viewModel.stateFlow.value.isLoading)
            assertEquals(1, collections)
            advanceTimeBy(99)
            runCurrent()
            assertEquals(1, collections)

            advanceUntilIdle()
            assertEquals(2, collections)
            assertEquals(1, maxActive)
            assertEquals(listOf(ada), viewModel.stateFlow.value.people)
            assertNull(viewModel.stateFlow.value.loadFailure)
            assertFalse(viewModel.stateFlow.value.isLoading)
         } finally {
            store.clear()
         }
         advanceUntilIdle()
         assertEquals(0, active)
      }

   @Test
   fun cancelledObservation_propagatesWithoutPublishingALoadFailure() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override fun observeAll(): Flow<Result<List<Person>>> = flow {
                  val cancellation = CancellationException("cancelled observation")
                  if (throwDirectly) throw cancellation
                  emit(Result.failure(cancellation))
               }
            }
            val store = ViewModelStore()
            val viewModel = createViewModel(repository).also { store.put("people", it) }
            val observer = viewModel.viewModelScope.coroutineContext[
               kotlinx.coroutines.Job
            ]!!.children.single()
            try {
               viewModel.effects.test {
                  advanceUntilIdle()
                  assertTrue(observer.isCancelled)
                  assertNull(viewModel.stateFlow.value.loadFailure)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
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
      val viewModel = createViewModel(repository)

      viewModel.onIntent(PeopleIntent.RetryLoad)
      viewModel.onIntent(PeopleIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()

      assertEquals(1, observations)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

}

/*
 * Didaktik und Lernziele
 *
 * - Tests unterscheiden einen gültigen leeren Datenbestand von einem Ladefehler.
 * - Ein fehlgeschlagener Flow bleibt beendet, bis RetryLoad ihn neu beobachtet.
 * - Retry während eines laufenden Versuchs startet keine zweite Beobachtung.
 */
