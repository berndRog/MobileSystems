package de.rogallab.mobile.ui.cars.input_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CarViewModelLoadTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val entity = Car(id = "c1", manufacturer = "Ford", model = "Focus", price = 10000,
      imagePaths = listOf("/images/focus.jpg"), personId = "p1")
   private val repository = FakeCarRepository(listOf(entity))
   private val people = ObservationProbe(listOf(PersonSelectItem("p1", "Lovelace, Ada")))
   private val imageEdit = FakeImageEdit()

   @After fun tearDown() { store.clear() }

   private fun create(id: String? = "c1"): CarViewModel {
      val peopleReadModel = object : IPersonReadModel {
         override fun observeListItems() =
            flowOf(Result.success(emptyList<de.rogallab.mobile.domain.readmodels.PersonListItem>()))
         override fun observeSelectItems() = people.observe()
      }
      return CarViewModel(
         carId = id,
         _carRepository = repository,
         _personReadModel = peopleReadModel,
         _stringProvider = FakeStringProvider(),
         _validator = CarValidator(context),
         _images = ImageEditDelegate(imageEdit, FakeImageFileStorage(), ImageSelectionMode.APPEND, MAX_CAR_IMAGE_COUNT),
         _carUcCreate = CarUcCreate(repository, imageEdit),
         _carUcUpdate = CarUcUpdate(repository, imageEdit),
         _effectDelegate = EffectDelegate(),
         _timing = AppTiming(delayRead = 0, delayWrite = 0),
      ).also { store.put("detail", it) }
   }

   private fun fillValidForm(vm: CarViewModel) {
      vm.onIntent(CarIntent.ManufacturerChanged("Ford"))
      vm.onIntent(CarIntent.ModelChanged("Focus"))
      vm.onIntent(CarIntent.SellerChanged("p1"))
      vm.onIntent(CarIntent.CameraImageTaken("/images/focus.jpg"))
   }

   @Test fun createMode_doesNotLoadExistingEntityAndUsesCreate() = runTest(dispatcherRule.testDispatcher) {
      val vm = create(null)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.isNew)
      assertFalse(vm.stateFlow.value.isLoading)
      assertTrue(repository.findCalls.isEmpty())
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(CarIntent.Save)
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
      assertEquals("Ford", vm.stateFlow.value.form.manufacturer)
      assertEquals("10000", vm.stateFlow.value.form.priceInput)
      vm.onIntent(CarIntent.Save)
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
      vm.onIntent(CarIntent.Save)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.Failed)
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
      vm.onIntent(CarIntent.RetryLoad)
      vm.onIntent(CarIntent.Save)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.NotFound)
      assertEquals(listOf("c1"), repository.findCalls)
      assertTrue(repository.created.isEmpty())
      assertTrue(repository.updated.isEmpty())
   }

   @Test fun successfulRetry_clearsFailureAndUpdatesOriginalId() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      repository.find = { Result.success(entity) }
      vm.onIntent(CarIntent.RetryLoad)
      assertTrue(vm.stateFlow.value.isLoading)
      vm.onIntent(CarIntent.Save)
      assertTrue(repository.updated.isEmpty())
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.loadFailure)
      assertEquals(entity.id, vm.stateFlow.value.form.id)
      vm.onIntent(CarIntent.Save)
      advanceUntilIdle()
      assertEquals(listOf(entity), repository.updated)
      assertTrue(repository.created.isEmpty())
      assertEquals(listOf("c1", "c1"), repository.findCalls)
   }

   @Test fun failedRetry_keepsTechnicalFailureAndUpdateMode() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(CarIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.Failed)
      assertFalse(vm.stateFlow.value.isLoading)
      assertFalse(vm.stateFlow.value.isNew)
      assertEquals(2, repository.findCalls.size)
   }

   @Test fun retryMayResolveToNotFound() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      repository.find = { Result.success(null) }
      vm.onIntent(CarIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.NotFound)
      assertFalse(vm.stateFlow.value.isLoading)
   }

   @Test fun repeatedDetailRetries_startOnlyOneLoad() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      advanceUntilIdle()
      val gate = CompletableDeferred<Unit>()
      repository.find = { gate.await(); Result.success(entity) }
      repeat(10) { vm.onIntent(CarIntent.RetryLoad) }
      runCurrent()
      repeat(10) { vm.onIntent(CarIntent.RetryLoad) }
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
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.Failed)
   }

   @Test fun cancellationResult_doesNotBecomeLoadFailureOrEnableSave() = runTest(dispatcherRule.testDispatcher) {
      repository.find = { Result.failure(CancellationException("cancelled")) }
      val vm = create()
      advanceUntilIdle()
      fillValidForm(vm)
      advanceUntilIdle()
      vm.onIntent(CarIntent.Save)
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
      vm.onIntent(CarIntent.RetryPeople)
      advanceUntilIdle()
      assertNull(vm.stateFlow.value.peopleLoadFailure)
      assertFalse(vm.stateFlow.value.isPeopleLoading)
      assertEquals(listOf(PersonSelectItem("p2", "Hopper, Grace")), vm.stateFlow.value.people)
      upstream.value = Result.success(emptyList())
      runCurrent()
      assertTrue(vm.stateFlow.value.people.isEmpty())
      assertEquals(originalForm, vm.stateFlow.value.form)
      assertEquals(1, repository.findCalls.size)
   }

   @Test fun peopleRetryFailure_remainsVisible() = runTest(dispatcherRule.testDispatcher) {
      people.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(CarIntent.RetryPeople)
      advanceUntilIdle()
      assertNotNull(vm.stateFlow.value.peopleLoadFailure)
      assertFalse(vm.stateFlow.value.isPeopleLoading)
      assertEquals(2, people.collections)
   }

   @Test fun repeatedPeopleRetries_waitForCleanupWithoutOverlap() = runTest(dispatcherRule.testDispatcher) {
      people.cleanupDelay = 100
      people.upstream = flowOf(Result.failure(IllegalStateException("offline")))
      val vm = create()
      runCurrent()
      people.upstream = MutableStateFlow(Result.success(listOf(PersonSelectItem("p2", "Hopper, Grace"))))
      repeat(10) { vm.onIntent(CarIntent.RetryPeople) }
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
      vm.onIntent(CarIntent.RetryPeople)
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
