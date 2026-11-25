package de.rogallab.mobile.ui.tdrives.input_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarReadModel
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.FakeTDriveRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.testing.ObservationProbe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class TDriveViewModelLoadTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val entity = TDrive("t1", "p1", "c1", Instant.parse("2026-01-01T12:00:00Z"), false)
   private val repository = FakeTDriveRepository(listOf(entity))
   private val people = ObservationProbe(listOf(PersonSelectItem("p1", "Lovelace, Ada")))
   private val cars = ObservationProbe(listOf(CarSelectItem("c1", "Ford Focus")))

   @After fun tearDown() { store.clear() }

   private fun create(id: String? = "t1"): TDriveViewModel {
      val peopleReadModel = object : IPersonReadModel {
         override fun observeListItems() =
            flowOf(Result.success(emptyList<de.rogallab.mobile.domain.readmodels.PersonListItem>()))
         override fun observeSelectItems() = people.observe()
      }
      val carReadModel = object : ICarReadModel by FakeCarReadModel() {
         override fun observeSelectItems() = cars.observe()
      }
      return TDriveViewModel(
         tDriveId = id,
         _tDriveRepository = repository,
         _personReadModel = peopleReadModel,
         _carReadModel = carReadModel,
         _stringProvider = FakeStringProvider(),
         _validator = TDriveValidator(context),
         _effectDelegate = EffectDelegate(),
         _timing = AppTiming(delayRead = 0, delayWrite = 0),
      ).also { store.put("detail", it) }
   }

   private fun fillValidForm(vm: TDriveViewModel) {
      vm.onIntent(TDriveIntent.PersonChanged("p1"))
      vm.onIntent(TDriveIntent.CarChanged("c1"))
      vm.onIntent(TDriveIntent.StartChanged("01.01.2026 12:00"))
   }

   @Test fun createMode_doesNotLoadExistingEntityAndUsesCreate() = runTest(dispatcherRule.testDispatcher) {
      val vm = create(null)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.isNew)
      assertFalse(vm.stateFlow.value.isLoading)
      assertTrue(repository.findCalls.isEmpty())
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertEquals(1, repository.created.size)
      assertTrue(repository.updated.isEmpty())
   }

   @Test fun successfulLoad_preservesIdAndUsesUpdate() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      assertTrue(vm.stateFlow.value.isLoading)
      advanceUntilIdle()
      assertFalse(vm.stateFlow.value.isNew)
      assertFalse(vm.stateFlow.value.isLoading)
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(entity.id, vm.stateFlow.value.form.id)
      assertEquals("p1", vm.stateFlow.value.form.personId)
      assertEquals(listOf(PersonSelectItem("p1", "Lovelace, Ada")), vm.stateFlow.value.people)
      assertEquals("c1", vm.stateFlow.value.form.carId)
      assertEquals(listOf(CarSelectItem("c1", "Ford Focus")), vm.stateFlow.value.cars)
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertEquals(listOf(entity), repository.updated)
      assertTrue(repository.created.isEmpty())
   }

   @Test fun technicalFailure_isPersistentAndBlocksSaveEvenWithValidInput() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is TDriveLoadFailure.Failed)
      assertFalse(vm.stateFlow.value.isLoading)
      assertFalse(vm.stateFlow.value.isNew)
      assertTrue(repository.created.isEmpty())
      assertTrue(repository.updated.isEmpty())
   }

   @Test fun notFound_isPersistentAndDoesNotAllowRetryOrSave() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.success(null) }
      val vm = create()
      advanceUntilIdle()
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.RetryLoad)
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is TDriveLoadFailure.NotFound)
      assertEquals(listOf("t1"), repository.findCalls)
      assertTrue(repository.created.isEmpty())
      assertTrue(repository.updated.isEmpty())
   }

   @Test fun successfulRetry_clearsFailureAndUpdatesOriginalId() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      repository.find = { Result.success(entity) }
      vm.onIntent(TDriveIntent.RetryLoad)
      assertTrue(vm.stateFlow.value.isLoading)
      vm.onIntent(TDriveIntent.Save)
      assertTrue(repository.updated.isEmpty())
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(entity.id, vm.stateFlow.value.form.id)
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertEquals(listOf(entity), repository.updated)
      assertTrue(repository.created.isEmpty())
      assertEquals(listOf("t1", "t1"), repository.findCalls)
   }

   @Test fun failedRetry_keepsTechnicalFailureAndUpdateMode() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is TDriveLoadFailure.Failed)
      assertFalse(vm.stateFlow.value.isLoading)
      assertFalse(vm.stateFlow.value.isNew)
      assertEquals(2, repository.findCalls.size)
   }

   @Test fun retryMayResolveToNotFound() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      repository.find = { Result.success(null) }
      vm.onIntent(TDriveIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is TDriveLoadFailure.NotFound)
      assertFalse(vm.stateFlow.value.isLoading)
   }

   @Test fun repeatedDetailRetries_startOnlyOneLoad() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      val gate = CompletableDeferred<Unit>()
      repository.find = { gate.await(); Result.success(entity) }
      repeat(10) { vm.onIntent(TDriveIntent.RetryLoad) }
      runCurrent()
      repeat(10) { vm.onIntent(TDriveIntent.RetryLoad) }
      assertEquals(2, repository.findCalls.size)
      assertTrue(vm.stateFlow.value.isLoading)
      gate.complete(Unit)
      advanceUntilIdle()
      assertEquals(entity.id, vm.stateFlow.value.form.id)
      assertNull(vm.stateFlow.value.loadFailure)
   }

   @Test fun thrownLoadException_isTechnicalFailureRatherThanNotFound() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { throw NoSuchElementException("query failed") }
      val vm = create()
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is TDriveLoadFailure.Failed)
   }

   @Test fun cancellationResult_doesNotBecomeLoadFailureOrEnableSave() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(CancellationException("cancelled")) }
      val vm = create()
      advanceUntilIdle()
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.Save)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertTrue(vm.stateFlow.value.isLoading)
      assertTrue(repository.created.isEmpty())
      assertTrue(repository.updated.isEmpty())
   }

   @Test fun clearingViewModel_cancelsPendingLoadWithoutLoadFailure() = runTest(dispatcherRule.testDispatcher) {
      var cancelled = false
      repository.find = {
         try { awaitCancellation() } finally { cancelled = true }
      }
      val vm = create()
      runCurrent()
      store.clear()
      advanceUntilIdle()
      assertTrue(cancelled)
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(0, people.active)
      assertEquals(0, cars.active)
   }

   @Test fun peopleFailure_stopsUntilSuccessfulRetryAndPreservesForm() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flow {
         emit(Result.failure(IllegalStateException("offline")))
         emit(Result.success(listOf(PersonSelectItem("p2", "Hopper, Grace"))))
      }
      val vm = create()
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.peopleLoadFailure)
      assertEquals(0, people.active)
      val originalForm = vm.stateFlow.value.form
      val upstream = MutableStateFlow(Result.success(listOf(PersonSelectItem("p2", "Hopper, Grace"))))
      people.upstream = upstream
      runCurrent()
      assertTrue(vm.stateFlow.value.people.isEmpty())
      vm.onIntent(TDriveIntent.RetryPeople)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertFalse(vm.stateFlow.value.isPeopleLoading)
      assertEquals(listOf(PersonSelectItem("p2", "Hopper, Grace")), vm.stateFlow.value.people)
      upstream.value = Result.success(emptyList())
      runCurrent()
      assertTrue(vm.stateFlow.value.people.isEmpty())
      assertEquals(originalForm, vm.stateFlow.value.form)
      assertEquals(1, repository.findCalls.size)
      assertEquals(1, cars.collections)
   }

   @Test fun peopleRetryFailure_remainsVisible() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.RetryPeople)
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.peopleLoadFailure)
      assertFalse(vm.stateFlow.value.isPeopleLoading)
      assertEquals(2, people.collections)
      assertEquals(1, cars.collections)
   }

   @Test fun repeatedPeopleRetries_waitForCleanupWithoutOverlap() = runTest(dispatcherRule.testDispatcher) {
      people.cleanupDelay = 100
      people.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      runCurrent()
      people.upstream = MutableStateFlow(Result.success(listOf(PersonSelectItem("p2", "Hopper, Grace"))))
      repeat(10) { vm.onIntent(TDriveIntent.RetryPeople) }
      runCurrent()
      assertEquals(1, people.collections)
      assertTrue(vm.stateFlow.value.isPeopleLoading)
      advanceTimeBy(100)
      runCurrent()
      assertEquals(2, people.collections)
      assertEquals(1, people.maxActive)
      assertEquals(listOf(PersonSelectItem("p2", "Hopper, Grace")), vm.stateFlow.value.people)
   }

   @Test fun peopleThrownException_canBeRetried() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flow { throw IllegalStateException("query failed") }
      val vm = create()
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.peopleLoadFailure)
      people.upstream = flowOf(Result.success(listOf(PersonSelectItem("p2", "Hopper, Grace"))))
      vm.onIntent(TDriveIntent.RetryPeople)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertEquals(listOf(PersonSelectItem("p2", "Hopper, Grace")), vm.stateFlow.value.people)
   }

   @Test fun peopleCancellationResult_isNotAnError() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flowOf(Result.failure(CancellationException("cancelled")))
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertEquals(1, people.completions)
      assertEquals(0, people.active)
   }

   @Test fun peopleThrownCancellation_isNotAnError() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flow { throw CancellationException("cancelled") }
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertEquals(1, people.completions)
   }

   @Test fun carsFailure_stopsUntilSuccessfulRetryAndPreservesForm() = runTest(dispatcherRule.testDispatcher) {
      cars.upstream = flow {
         emit(Result.failure(IllegalStateException("offline")))
         emit(Result.success(listOf(CarSelectItem("c2", "VW Golf"))))
      }
      val vm = create()
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(0, cars.active)
      val originalForm = vm.stateFlow.value.form
      val upstream = MutableStateFlow(Result.success(listOf(CarSelectItem("c2", "VW Golf"))))
      cars.upstream = upstream
      runCurrent()
      assertTrue(vm.stateFlow.value.cars.isEmpty())
      vm.onIntent(TDriveIntent.RetryCars)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.carsLoadFailure)
      assertFalse(vm.stateFlow.value.isCarsLoading)
      assertEquals(listOf(CarSelectItem("c2", "VW Golf")), vm.stateFlow.value.cars)
      upstream.value = Result.success(emptyList())
      runCurrent()
      assertTrue(vm.stateFlow.value.cars.isEmpty())
      assertEquals(originalForm, vm.stateFlow.value.form)
      assertEquals(1, repository.findCalls.size)
      assertEquals(1, people.collections)
   }

   @Test fun carsRetryFailure_remainsVisible() = runTest(dispatcherRule.testDispatcher) {
      cars.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(TDriveIntent.RetryCars)
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.carsLoadFailure)
      assertFalse(vm.stateFlow.value.isCarsLoading)
      assertEquals(2, cars.collections)
      assertEquals(1, people.collections)
   }

   @Test fun repeatedCarsRetries_waitForCleanupWithoutOverlap() = runTest(dispatcherRule.testDispatcher) {
      cars.cleanupDelay = 100
      cars.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      runCurrent()
      cars.upstream = MutableStateFlow(Result.success(listOf(CarSelectItem("c2", "VW Golf"))))
      repeat(10) { vm.onIntent(TDriveIntent.RetryCars) }
      runCurrent()
      assertEquals(1, cars.collections)
      assertTrue(vm.stateFlow.value.isCarsLoading)
      advanceTimeBy(100)
      runCurrent()
      assertEquals(2, cars.collections)
      assertEquals(1, cars.maxActive)
      assertEquals(listOf(CarSelectItem("c2", "VW Golf")), vm.stateFlow.value.cars)
   }

   @Test fun carsThrownException_canBeRetried() = runTest(dispatcherRule.testDispatcher) {
      cars.upstream = flow { throw IllegalStateException("query failed") }
      val vm = create()
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.carsLoadFailure)
      cars.upstream = flowOf(Result.success(listOf(CarSelectItem("c2", "VW Golf"))))
      vm.onIntent(TDriveIntent.RetryCars)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(listOf(CarSelectItem("c2", "VW Golf")), vm.stateFlow.value.cars)
   }

   @Test fun carsCancellationResult_isNotAnError() = runTest(dispatcherRule.testDispatcher) {
      cars.upstream = flowOf(Result.failure(CancellationException("cancelled")))
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(1, cars.completions)
      assertEquals(0, cars.active)
   }

   @Test fun carsThrownCancellation_isNotAnError() = runTest(dispatcherRule.testDispatcher) {
      cars.upstream = flow { throw CancellationException("cancelled") }
      val vm = create()
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(1, cars.completions)
   }

   @Test fun selectionRetries_areIndependentWhenBothFail() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flowOf(Result.failure(IllegalStateException("people offline")))
      cars.upstream = flowOf(Result.failure(IllegalStateException("cars offline")))
      val vm = create()
      advanceUntilIdle()
      people.upstream = MutableStateFlow(Result.success(listOf(PersonSelectItem("p1", "Lovelace, Ada"))))
      vm.onIntent(TDriveIntent.RetryPeople)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertNotNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(2, people.collections)
      assertEquals(1, cars.collections)
      cars.upstream = MutableStateFlow(Result.success(listOf(CarSelectItem("c1", "Ford Focus"))))
      vm.onIntent(TDriveIntent.RetryCars)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.carsLoadFailure)
      assertEquals(2, people.collections)
      assertEquals(2, cars.collections)
      assertEquals(1, repository.findCalls.size)
   }

   @Test fun lateRepositoryOutcomeAfterCancellation_doesNotChangeState() = runTest(dispatcherRule.testDispatcher) {
      for (outcome in listOf("success", "failure", "throw")) {
         repository.find = {
            // Simulate an upstream operation that finishes despite cancellation.
            withContext(NonCancellable) { delay(100) }
            when (outcome) {
               "success" -> Result.success(entity)
               "failure" -> Result.failure(IllegalStateException("late failure"))
               else -> throw IllegalStateException("late exception")
            }
         }
         val vm = create()
         runCurrent()
         val beforeCancellation = vm.stateFlow.value
         store.clear()
         advanceUntilIdle()
         assertEquals(beforeCancellation, vm.stateFlow.value)
      }
   }
}
