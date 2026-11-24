package de.rogallab.mobile.ui.tdrives.list

import androidx.lifecycle.ViewModelStore
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import de.rogallab.mobile.domain.readmodels.ITDriveReadModel
import de.rogallab.mobile.domain.usecases.TDriveUcDelete
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TDrivesViewModelLoadTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val item = TDriveListItem("t1", "Ada Lovelace", "Ford Focus", Instant.parse("2026-01-01T12:00:00Z"), false)
   private val probe = ObservationProbe<TDriveListItem>()
   private val strings = FakeStringProvider()

   @After fun tearDown() { store.clear() }

   private fun create(): TDrivesViewModel {
      val readModel = object : ITDriveReadModel by FakeTDriveReadModel() {
         override fun observeListItems() = probe.observe()
      }
      return TDrivesViewModel(
         _readModel = readModel,
         _tDriveUcDelete = TDriveUcDelete(FakeTDriveRepository()),
         _stringProvider = strings,
         _effectDelegate = EffectDelegate(),
         _timing = AppTiming(delayRead = 0, delayWrite = 0),
      ).also { store.put("list", it) }
   }

   @Test fun successfulObservation_keepsEmittingAndIgnoresRetryWhileHealthy() = runTest(dispatcherRule.testDispatcher) {
      val upstream = MutableStateFlow(Result.success(listOf(item)))
      probe.upstream = upstream
      val vm = create()
      assertTrue(vm.stateFlow.value.isLoading)
      runCurrent()
      assertEquals(listOf(item), vm.stateFlow.value.tDrives)
      assertNull(vm.stateFlow.value.loadFailure)
      vm.onIntent(TDrivesIntent.RetryLoad)
      upstream.value = Result.success(emptyList())
      runCurrent()
      assertEquals(emptyList<TDriveListItem>(), vm.stateFlow.value.tDrives)
      assertEquals(1, probe.collections)
   }

   @Test fun resultFailure_stopsCollectorAndKeepsPersistentError() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flow {
         emit(Result.failure(IllegalStateException("offline")))
         emit(Result.success(listOf(item)))
      }
      val vm = create()
      advanceUntilIdle()
      assertEquals(strings.getString(R.string.error_test_drives_load), vm.stateFlow.value.loadFailure)
      assertFalse(vm.stateFlow.value.isLoading)
      assertTrue(vm.stateFlow.value.tDrives.isEmpty())
      assertEquals(0, probe.active)
      assertEquals(1, probe.completions)
   }

   @Test fun retry_resubscribesAndReceivesFurtherUpdates() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      advanceUntilIdle()
      val upstream = MutableStateFlow(Result.success(listOf(item)))
      probe.upstream = upstream
      vm.onIntent(TDrivesIntent.RetryLoad)
      assertTrue(vm.stateFlow.value.isLoading)
      assertNull(vm.stateFlow.value.loadFailure)
      runCurrent()
      assertEquals(listOf(item), vm.stateFlow.value.tDrives)
      upstream.value = Result.success(emptyList())
      runCurrent()
      assertTrue(vm.stateFlow.value.tDrives.isEmpty())
      assertFalse(vm.stateFlow.value.isLoading)
      assertEquals(2, probe.collections)
   }

   @Test fun failedRetry_keepsErrorVisible() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDrivesIntent.RetryLoad)
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.loadFailure)
      assertFalse(vm.stateFlow.value.isLoading)
      assertEquals(2, probe.collections)
   }

   @Test fun repeatedRetries_waitForUpstreamCleanupAndNeverOverlap() = runTest(dispatcherRule.testDispatcher) {
      probe.cleanupDelay = 100
      probe.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      runCurrent()
      assertNotNull(vm.stateFlow.value.loadFailure)
      assertEquals(1, probe.active)
      probe.upstream = MutableStateFlow(Result.success(listOf(item)))
      repeat(10) { vm.onIntent(TDrivesIntent.RetryLoad) }
      runCurrent()
      assertEquals(1, probe.collections)
      assertTrue(vm.stateFlow.value.isLoading)
      advanceTimeBy(100)
      runCurrent()
      assertEquals(2, probe.collections)
      assertEquals(1, probe.maxActive)
      assertEquals(listOf(item), vm.stateFlow.value.tDrives)
   }

   @Test fun thrownFlowException_becomesRetryableError() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flow { throw IllegalStateException("query failed") }
      val vm = create()
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.loadFailure)
      probe.upstream = flowOf(Result.success(listOf(item)))
      vm.onIntent(TDrivesIntent.RetryLoad)
      advanceUntilIdle()
      assertEquals(listOf(item), vm.stateFlow.value.tDrives)
      assertNull(vm.stateFlow.value.loadFailure)
   }

   @Test fun cancellationInResult_isNotALoadFailure() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flowOf(Result.failure(CancellationException("cancelled")))
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(1, probe.completions)
      assertEquals(0, probe.active)
   }

   @Test fun thrownCancellation_isNotALoadFailure() = runTest(dispatcherRule.testDispatcher) {
      probe.upstream = flow { throw CancellationException("cancelled") }
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(1, probe.completions)
   }

   @Test fun clearingViewModel_cancelsObservationWithoutError() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      runCurrent()
      assertEquals(1, probe.active)
      store.clear()
      advanceUntilIdle()
      assertEquals(0, probe.active)
      assertEquals(1, probe.completions)
      assertNull(vm.stateFlow.value.loadFailure)
   }
}
