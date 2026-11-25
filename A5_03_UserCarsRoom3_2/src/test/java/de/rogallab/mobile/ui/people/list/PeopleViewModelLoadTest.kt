package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModelStore
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.testing.ObservationProbe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelLoadTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val item = PersonListItem("p1", "Ada Lovelace", null, null, null)
   private val probe = ObservationProbe(listOf(item))
   private val readModel = object : IPersonReadModel {
      override fun observeListItems() = probe.observe()
      override fun observeSelectItems() = flowOf(Result.success(emptyList<de.rogallab.mobile.domain.readmodels.PersonSelectItem>()))
   }

   private fun create() = PeopleViewModel(
      _readModel = readModel,
      _personUcDelete = PersonUcDelete(FakePersonRepository(), FakeImageFileStorage()),
      _stringProvider = FakeStringProvider(),
      _effectDelegate = EffectDelegate(),
      _timing = de.rogallab.mobile.AppTiming(delayRead = 0, delayWrite = 0),
   ).also { store.put("people", it) }

   @After fun tearDown() { store.clear() }

   @Test fun failureIsPublishedBeforeCleanup_andRepeatedRetryWaitsWithoutOverlap() =
      runTest(dispatcherRule.testDispatcher) {
         probe.cleanupDelay = 100
         probe.upstream = flow {
            emit(Result.failure(IllegalStateException("offline")))
            emit(Result.success(listOf(item)))
         }
         val vm = create()
         assertTrue(vm.stateFlow.value.isLoading)
         runCurrent()
         assertNotNull(vm.stateFlow.value.loadFailure)
         assertFalse(vm.stateFlow.value.isLoading)
         assertTrue(vm.stateFlow.value.listItems.isEmpty())
         assertEquals(1, probe.active)
         val upstream = MutableStateFlow(Result.success(listOf(item)))
         probe.upstream = upstream
         repeat(10) { vm.onIntent(PeopleIntent.RetryLoad) }
         assertTrue(vm.stateFlow.value.isLoading)
         assertNull(vm.stateFlow.value.loadFailure)
         runCurrent()
         assertEquals(1, probe.collections)
         advanceTimeBy(100)
         runCurrent()
         assertEquals(2, probe.collections)
         assertEquals(1, probe.maxActive)
         assertEquals(listOf(item), vm.stateFlow.value.listItems)
         vm.onIntent(PeopleIntent.RetryLoad) // Healthy observation must keep running.
         upstream.value = Result.success(emptyList())
         runCurrent()
         assertTrue(vm.stateFlow.value.listItems.isEmpty())
         assertEquals(2, probe.collections)
      }

   @Test fun thrownFlowFailure_andFailedRetry_remainRetryable() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flow { throw IllegalStateException("query failed") }
      val vm = create()
      runCurrent()
      assertNotNull(vm.stateFlow.value.loadFailure)
      probe.upstream = flowOf(Result.failure(IllegalStateException("still offline")))
      vm.onIntent(PeopleIntent.RetryLoad)
      runCurrent()
      assertNotNull(vm.stateFlow.value.loadFailure)
      assertFalse(vm.stateFlow.value.isLoading)
      probe.upstream = flowOf(Result.success(listOf(item)))
      vm.onIntent(PeopleIntent.RetryLoad)
      runCurrent()
      assertEquals(listOf(item), vm.stateFlow.value.listItems)
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(3, probe.collections)
   }

   @Test fun cancellationResultsAndThrownCancellation_areNotLoadFailures() =
      runTest(dispatcherRule.testDispatcher) {
         for (throws in listOf(false, true)) {
            probe.upstream = flow {
               if (throws) throw CancellationException("cancelled")
               emit(Result.failure(CancellationException("cancelled")))
            }
            val vm = create()
            runCurrent()
            assertNull(vm.stateFlow.value.loadFailure)
            assertEquals(0, probe.active)
         }
      }

   @Test fun clearingViewModel_stopsObservationWithoutPublishingAnError() =
      runTest(dispatcherRule.testDispatcher) {
         val vm = create()
         runCurrent()
         assertEquals(1, probe.active)
         store.clear()
         advanceUntilIdle()
         assertEquals(0, probe.active)
         assertNull(vm.stateFlow.value.loadFailure)
      }
}
