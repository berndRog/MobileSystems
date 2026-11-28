package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.R
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
            return source.observeAll()
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
   fun resultFailureBecomesPersistentFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("read failed"))
      }
      val stringProvider = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, stringProvider, EffectDelegate())
      viewModel.effects.test {
         advanceUntilIdle()
         assertEquals(stringProvider.getString(R.string.error_people_observe),
            viewModel.stateFlow.value.loadFailure)
         assertFalse(viewModel.stateFlow.value.isLoading)
         expectNoEvents()
      }
   }
   @Test
   fun remove_emitsExactlyOneTypedResultMessage() = runTest(mainDispatcherRule.testDispatcher) {
      val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")
      val strings = FakeStringProvider()
      for (succeeds in listOf(true, false)) {
         val repository = FakePersonRepository(listOf(ada)).apply {
            if (!succeeds) removeResult = Result.failure(IllegalStateException("delete failed"))
         }
         val viewModel = PeopleViewModel(repository, strings, EffectDelegate())
         val store = ViewModelStore().also { it.put("people", viewModel) }
         try {
            advanceUntilIdle()
            viewModel.effects.test {
               viewModel.onIntent(PeopleIntent.Remove(ada))
               advanceUntilIdle()
               val expected = if (succeeds) PeopleEffect.ShowMessage(
                  strings.getString(R.string.message_person_removed, ada.fullName)
               ) else PeopleEffect.ShowError(strings.getString(R.string.error_person_remove))
               assertEquals(expected, awaitItem())
               assertEquals(if (succeeds) listOf(ada) else emptyList<Person>(), repository.removed)
               expectNoEvents()
            }
         } finally {
            store.clear()
         }
      }
   }

   @Test
   fun cancelledRemove_hasNoErrorMessage_whetherThrownOrReturned() =
      runTest(mainDispatcherRule.testDispatcher) {
         val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")
         for (throwDirectly in listOf(false, true)) {
            val source = FakePersonRepository(listOf(ada))
            val repository = object : IPersonRepository by source {
               override suspend fun remove(person: Person): Result<Unit> {
                  val cancellation = CancellationException("delete cancelled")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = PeopleViewModel(repository, FakeStringProvider(), EffectDelegate())
            val store = ViewModelStore().also { it.put("people", viewModel) }
            try {
               advanceUntilIdle()
               viewModel.effects.test {
                  viewModel.onIntent(PeopleIntent.Remove(ada))
                  advanceUntilIdle()
                  assertTrue(source.removed.isEmpty())
                  assertEquals(listOf(ada), viewModel.stateFlow.value.people)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun cancelledObservation_isNotAStoredFailureOrMessage_whetherThrownOrReturned() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override fun observeAll(): Flow<Result<List<Person>>> = flow {
                  val cancellation = CancellationException("observation cancelled")
                  if (throwDirectly) throw cancellation
                  emit(Result.failure(cancellation))
               }
            }
            val viewModel = PeopleViewModel(repository, FakeStringProvider(), EffectDelegate())
            val store = ViewModelStore().also { it.put("people", viewModel) }
            val observing = viewModel.viewModelScope.coroutineContext[Job]!!.children.single()
            try {
               viewModel.effects.test {
                  advanceUntilIdle()
                  assertTrue(observing.isCancelled)
                  assertNull(viewModel.stateFlow.value.loadFailure)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

}

/*
 * Didaktik und Lernziele
 *
 * - Erfolgreiche Room-Emissionen aktualisieren den State fortlaufend.
 * - Nach einem Ladefehler startet erst RetryLoad einen neuen Collector.
 */
