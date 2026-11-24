package de.rogallab.mobile.ui.cars.input_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.*
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CarViewModelSaveTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val entity = Car(id = "c1", manufacturer = "Ford", model = "Focus", price = 10000,
      personId = "p1", imagePaths = listOf("/images/focus.jpg"))
   private val backing = FakeCarRepository(listOf(entity))
   private val creates = mutableListOf<Car>()
   private val updates = mutableListOf<Car>()
   private var write: suspend (Car) -> Result<Unit> = { Result.success(Unit) }
   private val imageEdit = FakeImageEdit()
   private val repository = object : ICarRepository by backing {
      override suspend fun create(car: Car): Result<Unit> {
         creates += car
         return write(car)
      }
      override suspend fun update(car: Car): Result<Unit> {
         updates += car
         return write(car)
      }
   }

   @After fun tearDown() { store.clear() }

   private fun create(id: String? = "c1", delayWrite: Long = 50): CarViewModel {
      val people = object : IPersonReadModel {
         override fun observeListItems() = flowOf(Result.success(emptyList<PersonListItem>()))
         override fun observeSelectItems() = flowOf(Result.success(emptyList<PersonSelectItem>()))
      }
      return CarViewModel(
         carId = id,
         _carRepository = repository,
         _personReadModel = people,
         _imageFileStorage = FakeImageFileStorage(),
         _imageEdit = imageEdit,
         _carUcCreate = CarUcCreate(repository, imageEdit),
         _carUcUpdate = CarUcUpdate(repository, imageEdit),
         _validator = CarValidator(context),
         _stringProvider = FakeStringProvider(),
         _effectDelegate = EffectDelegate(),
         _timing = AppTiming(delayRead = 0, delayWrite = delayWrite),
      ).also { store.put("detail", it) }
   }

   private fun fillCreateForm(vm: CarViewModel) {
      vm.onIntent(CarIntent.ManufacturerChanged(" Ford "))
      vm.onIntent(CarIntent.ModelChanged(" Focus "))
      vm.onIntent(CarIntent.PriceChanged("10000"))
      vm.onIntent(CarIntent.SellerChanged("p1"))
      vm.onIntent(CarIntent.CameraImageTaken("/images/focus.jpg"))
   }

   @Test fun createSave_setsStateSynchronouslyAndNavigatesExactlyOnce() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create(null)
      fillCreateForm(vm)
      advanceUntilIdle()
      assertFalse(vm.stateFlow.value.isSaving)
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         assertTrue(creates.isEmpty())
         advanceUntilIdle()
         assertTrue(vm.stateFlow.value.isSaving)
         assertEquals(1, creates.size)
         expectNoEvents()
         gate.complete(Result.success(Unit))
         runCurrent()
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
         assertTrue(updates.isEmpty())
      }
   }

   @Test fun updateSave_writesOriginalSnapshotAndNavigatesOnce() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
         advanceUntilIdle()
         assertEquals(listOf(entity), updates)
         assertTrue(creates.isEmpty())
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun duplicateSaveIntents_duringDelayAndWriteStartOnlyOneOperation() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(CarIntent.Save)
      repeat(10) { vm.onIntent(CarIntent.Save) }
      runCurrent()
      assertTrue(updates.isEmpty())
      advanceTimeBy(50)
      runCurrent()
      repeat(10) { vm.onIntent(CarIntent.Save) }
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
      vm.onIntent(CarIntent.Save)
      repeat(2) {
         vm.onIntent(CarIntent.ManufacturerChanged("VW"))
         vm.onIntent(CarIntent.ModelChanged("Golf"))
         vm.onIntent(CarIntent.PriceChanged("999"))
         vm.onIntent(CarIntent.SellerChanged("p2"))
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
         vm.onIntent(CarIntent.Save)
         // The adapters translate System/Predictive Back into the same Cancel intent.
         repeat(2) { vm.onIntent(CarIntent.Cancel) }
         advanceUntilIdle()
         repeat(2) { vm.onIntent(CarIntent.Cancel) }
         runCurrent()
         expectNoEvents()
         assertEquals(0, imageEdit.discardCount)
         assertTrue(vm.stateFlow.value.isSaving)
         gate.complete(Result.success(Unit))
         runCurrent()
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
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
         vm.onIntent(CarIntent.Save)
         advanceUntilIdle()
         gate.complete(Result.failure(IllegalStateException("write failed")))
         runCurrent()
         assertFalse(vm.stateFlow.value.isSaving)
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertEquals(CarEffect.ShowError(FakeStringProvider().getString(R.string.error_car_save)), awaitItem())
         expectNoEvents()
         assertEquals(0, imageEdit.commitCount)
         vm.onIntent(CarIntent.ModelChanged("Fiesta"))
         assertEquals("Fiesta", vm.stateFlow.value.form.model)
         write = { Result.success(Unit) }
         vm.onIntent(CarIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         advanceUntilIdle()
         assertEquals(2, updates.size)
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun thrownWriteException_resetsSavingAndKeepsForm() = runTest(dispatcherRule.testDispatcher) {
      write = { throw IllegalStateException("storage failed") }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertTrue(awaitItem() is CarEffect.ShowError)
         expectNoEvents()
      }
   }

   @Test fun cancellationResult_isPropagatedWithoutErrorOrNavigation() = runTest(dispatcherRule.testDispatcher) {
      write = { Result.failure(CancellationException("cancelled")) }
      val vm = create()
      advanceUntilIdle()
      val snapshot = vm.stateFlow.value.form
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
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
         vm.onIntent(CarIntent.Save)
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
         vm.onIntent(CarIntent.Save)
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
         vm.onIntent(CarIntent.Save)
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
         vm.onIntent(CarIntent.Cancel)
         vm.onIntent(CarIntent.Save)
         vm.onIntent(CarIntent.Cancel)
         advanceUntilIdle()
         assertTrue(updates.isEmpty())
         assertEquals(CarEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
         assertEquals(1, imageEdit.discardCount)
      }
   }

   @Test fun completedSave_blocksFurtherActionsUntilNavigationIsHandled() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(CarIntent.Save)
      advanceUntilIdle()
      // Effects are buffered; the adapter has not consumed NavigateBack yet.
      vm.onIntent(CarIntent.Save)
      vm.onIntent(CarIntent.Cancel)
      advanceUntilIdle()
      vm.effects.test {
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
         expectNoEvents()
      }
      assertEquals(1, updates.size)
      assertEquals(0, imageEdit.discardCount)
   }

   @Test fun loadFailureAndNotFound_stillBlockSaving() = runTest(dispatcherRule.testDispatcher) {
      for (result in listOf(Result.failure<Car?>(IllegalStateException("offline")), Result.success(null))) {
         backing.find = { result }
         val vm = create()
         advanceUntilIdle()
         fillCreateForm(vm)
         advanceUntilIdle()
         vm.onIntent(CarIntent.Save)
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
         vm.onIntent(CarIntent.Save)
         advanceUntilIdle()
         assertFalse(vm.stateFlow.value.isSaving)
         assertTrue(creates.isEmpty())
         assertTrue(awaitItem() is CarEffect.ShowError)
         expectNoEvents()
      }
   }

   @Test fun cancelAfterWriteFailure_isAllowed() = runTest(dispatcherRule.testDispatcher) {
      write = { Result.failure(IllegalStateException("write failed")) }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
         advanceUntilIdle()
         assertTrue(awaitItem() is CarEffect.ShowError)
         vm.onIntent(CarIntent.Cancel)
         advanceUntilIdle()
         assertEquals(CarEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }
}
