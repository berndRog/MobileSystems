package de.rogallab.mobile.ui.tdrives.input_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.repositories.ITDriveRepository
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarReadModel
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.FakeTDriveRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TDriveViewModelSaveTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val entity = TDrive("t1", "p1", "c1", Instant.parse("2026-01-01T12:00:00Z"), false)
   private val backing = FakeTDriveRepository(listOf(entity))
   private val creates = mutableListOf<TDrive>()
   private val updates = mutableListOf<TDrive>()
   private var write: suspend (TDrive) -> Result<Unit> = { Result.success(Unit) }
   private val repository = object : ITDriveRepository by backing {
      override suspend fun create(tDrive: TDrive): Result<Unit> {
         creates += tDrive
         return write(tDrive)
      }
      override suspend fun update(tDrive: TDrive): Result<Unit> {
         updates += tDrive
         return write(tDrive)
      }
   }

   @After fun tearDown() { store.clear() }

   private fun create(id: String? = "t1", delayWrite: Long = 50): TDriveViewModel {
      val people = object : IPersonReadModel {
         override fun observeListItems() = flowOf(Result.success(emptyList<PersonListItem>()))
         override fun observeSelectItems() = flowOf(Result.success(emptyList<PersonSelectItem>()))
      }
      return TDriveViewModel(
         tDriveId = id,
         _tDriveRepository = repository,
         _personReadModel = people,
         _carReadModel = FakeCarReadModel(),
         _validator = TDriveValidator(context),
         _stringProvider = FakeStringProvider(),
         _effectDelegate = EffectDelegate(),
         _timing = AppTiming(delayRead = 0, delayWrite = delayWrite),
      ).also { store.put("detail", it) }
   }

   private fun fillCreateForm(vm: TDriveViewModel) {
      vm.onIntent(TDriveIntent.PersonChanged("p1"))
      vm.onIntent(TDriveIntent.CarChanged("c1"))
      vm.onIntent(TDriveIntent.StartChanged("01.01.2026 12:00"))
   }

   @Test fun createSave_setsStateSynchronouslyAndNavigatesExactlyOnce() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create(null)
      fillCreateForm(vm)
      advanceUntilIdle()
      assertFalse(vm.stateFlow.value.isSaving)
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         assertTrue(creates.isEmpty())
         advanceUntilIdle()
         assertTrue(vm.stateFlow.value.isSaving)
         assertEquals(1, creates.size)
         expectNoEvents()
         gate.complete(Result.success(Unit))
         runCurrent()
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is TDriveEffect.ShowMessage)
         assertEquals(TDriveEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
         assertTrue(updates.isEmpty())
      }
   }

   @Test fun updateSave_writesOriginalSnapshotAndNavigatesOnce() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertEquals(listOf(entity), updates)
         assertTrue(creates.isEmpty())
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is TDriveEffect.ShowMessage)
         assertEquals(TDriveEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun duplicateSaveIntents_duringDelayAndWriteStartOnlyOneOperation() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.Save)
      repeat(10) { vm.onIntent(TDriveIntent.Save) }
      runCurrent()
      assertTrue(updates.isEmpty())
      advanceTimeBy(50)
      runCurrent()
      repeat(10) { vm.onIntent(TDriveIntent.Save) }
      runCurrent()
      assertEquals(listOf(entity), updates)
      gate.complete(Result.success(Unit))
      advanceUntilIdle()
      assertEquals(1, updates.size)
   }

   @Test fun ordinaryFormIntents_duringSaveCannotChangeSnapshot() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.onIntent(TDriveIntent.Save)
      repeat(2) {
         vm.onIntent(TDriveIntent.PersonChanged("p2"))
         vm.onIntent(TDriveIntent.CarChanged("c2"))
         vm.onIntent(TDriveIntent.StartChanged("02.01.2026 13:00"))
         vm.onIntent(TDriveIntent.CompletedChanged(true))
         assertEquals(snapshot, vm.stateFlow.value.form)
         advanceUntilIdle()
      }
      assertEquals(listOf(entity), updates)
      gate.complete(Result.success(Unit))
      advanceUntilIdle()
      assertEquals(snapshot, vm.stateFlow.value.form)
   }

   @Test fun cancelAndBackIntent_duringSaveDoNotNavigate() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         // The adapters translate System/Predictive Back into the same Cancel intent.
         repeat(2) { vm.onIntent(TDriveIntent.Cancel) }
         advanceUntilIdle()
         repeat(2) { vm.onIntent(TDriveIntent.Cancel) }
         runCurrent()
         expectNoEvents()
         assertTrue(vm.stateFlow.value.isSaving)
         gate.complete(Result.success(Unit))
         runCurrent()
         assertTrue(awaitItem() is TDriveEffect.ShowMessage)
         assertEquals(TDriveEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun failedWrite_preservesFormAllowsEditingAndCanBeRetried() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         gate.complete(Result.failure(IllegalStateException("write failed")))
         runCurrent()
         assertFalse(vm.stateFlow.value.isSaving)
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertEquals(TDriveEffect.ShowError(FakeStringProvider().getString(R.string.error_test_drive_save)), awaitItem())
         expectNoEvents()
         vm.onIntent(TDriveIntent.CompletedChanged(true))
         assertTrue(vm.stateFlow.value.form.isCompleted)
         write = { Result.success(Unit) }
         vm.onIntent(TDriveIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertEquals(2, updates.size)
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is TDriveEffect.ShowMessage)
         assertEquals(TDriveEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun thrownWriteException_resetsSavingAndKeepsForm() = runTest(dispatcherRule.testDispatcher) {
      write = { throw IllegalStateException("storage failed") }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertTrue(awaitItem() is TDriveEffect.ShowError)
         expectNoEvents()
      }
   }

   @Test fun cancellationResult_isPropagatedWithoutErrorOrNavigation() = runTest(dispatcherRule.testDispatcher) {
      write = { Result.failure(CancellationException("cancelled")) }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertEquals(snapshot, vm.stateFlow.value.form)
         expectNoEvents()
      }
   }

   @Test fun thrownCancellation_resetsSavingWithoutErrorOrNavigation() = runTest(dispatcherRule.testDispatcher) {
      write = { throw CancellationException("cancelled") }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         expectNoEvents()
      }
   }

   @Test fun clearingDuringWrite_cancelsUpstreamAndResetsSaving() = runTest(dispatcherRule.testDispatcher) {
      var cancelled = false
      write = {
         try { awaitCancellation() } finally { cancelled = true }
      }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertTrue(vm.stateFlow.value.isSaving)
         store.clear()
         advanceUntilIdle()
         assertTrue(cancelled)
         assertFalse(vm.stateFlow.value.isSaving)
         expectNoEvents()
      }
   }

   @Test fun clearingBeforeDispatch_stillResetsSynchronousSavingFlag() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         store.clear()
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(updates.isEmpty())
         expectNoEvents()
      }
   }

   @Test fun acceptedCancel_blocksQueuedSaveAndDuplicateNavigation() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Cancel)
         vm.onIntent(TDriveIntent.Save)
         vm.onIntent(TDriveIntent.Cancel)
         advanceUntilIdle()
         assertTrue(updates.isEmpty())
         assertEquals(TDriveEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun completedSave_blocksFurtherActionsUntilNavigationIsHandled() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      // Effects are buffered; the adapter has not consumed NavigateBack yet.
      vm.onIntent(TDriveIntent.Save)
      vm.onIntent(TDriveIntent.Cancel)
      advanceUntilIdle()
      vm.effects.test {
         assertTrue(awaitItem() is TDriveEffect.ShowMessage)
         assertEquals(TDriveEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
      assertEquals(1, updates.size)
   }

   @Test fun loadFailureAndNotFound_stillBlockSaving() = runTest(dispatcherRule.testDispatcher) {
      for (result in listOf(Result.failure<TDrive?>(IllegalStateException("offline")), Result.success(null))) {
         backing.find = { result }
         val vm = create()
         advanceUntilIdle()
         fillCreateForm(vm)
         advanceUntilIdle()
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertNotNull(vm.stateFlow.value.loadFailure)
         assertTrue(creates.isEmpty())
         assertTrue(updates.isEmpty())
         store.clear()
      }
   }

   @Test fun validationFailure_doesNotEnterSavingState() = runTest(dispatcherRule.testDispatcher) {
      val vm = create(null)
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(creates.isEmpty())
         assertTrue(awaitItem() is TDriveEffect.ShowError)
         expectNoEvents()
      }
   }

   @Test fun cancelAfterWriteFailure_isAllowed() = runTest(dispatcherRule.testDispatcher) {
      write = { Result.failure(IllegalStateException("write failed")) }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(TDriveIntent.Save)
         advanceUntilIdle()
         assertTrue(awaitItem() is TDriveEffect.ShowError)
         vm.onIntent(TDriveIntent.Cancel)
         advanceUntilIdle()
         assertEquals(TDriveEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }
}
