package de.rogallab.mobile.ui.cars.input_detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.testing.FakeCarRepository
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CarViewModelImageTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val original = "/images/original.jpg"
   private val photo = "/images/photo.jpg"
   private val entity = Car(id = "c1", manufacturer = "Ford", model = "Focus",
      personId = "p1", imagePaths = listOf(original))

   // Models file existence and suspending storage without touching real files.
   private class Storage(original: String) : IImageFileStorage by FakeImageFileStorage() {
      val files = mutableSetOf(original)
      val copied = mutableListOf<Uri>()
      val deleted = mutableListOf<String>()
      val events = mutableListOf<String>()
      var copy: suspend (Uri) -> Result<String> = {
         Result.success("/images/${it.lastPathSegment}.jpg")
      }
      var beforeDelete: suspend (String) -> Result<Unit> = { Result.success(Unit) }

      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> {
         copied += sourceUri
         return copy(sourceUri).onSuccess { files += it }
      }
      override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
         if (imagePath == null) return Result.success(Unit)
         deleted += imagePath
         events += "delete:$imagePath"
         return beforeDelete(imagePath).onSuccess { files -= imagePath }
      }
   }

   private val storage = Storage(original)
   private val edit = ImageEdit(storage)
   private val backing = FakeCarRepository(listOf(entity))
   private val writes = mutableListOf<Car>()
   private var write: suspend (Car) -> Result<Unit> = { Result.success(Unit) }
   private val repository = object : ICarRepository by backing {
      override suspend fun create(car: Car) = persist(car)
      override suspend fun update(car: Car) = persist(car)
      private suspend fun persist(car: Car): Result<Unit> {
         writes += car
         assertEquals(car.imagePaths, edit.add(emptyList()))
         assertTrue(storage.files.containsAll(car.imagePaths))
         return write(car).onSuccess { storage.events += "persisted" }
      }
   }

   @After fun tearDown() { store.clear() }

   private fun create(id: String? = "c1"): CarViewModel {
      val people = object : IPersonReadModel {
         override fun observeListItems() = flowOf(Result.success(emptyList<PersonListItem>()))
         override fun observeSelectItems() = flowOf(Result.success(emptyList<PersonSelectItem>()))
      }
      val vm = CarViewModel(
         carId = id, _carRepository = repository, _personReadModel = people,
         _stringProvider = FakeStringProvider(),
         _validator = CarValidator(ApplicationProvider.getApplicationContext<android.content.Context>()),
         _images = ImageEditDelegate(edit, storage, ImageSelectionMode.APPEND, MAX_CAR_IMAGE_COUNT),
         _carUcCreate = CarUcCreate(repository, edit), _carUcUpdate = CarUcUpdate(repository, edit),
         _effectDelegate = EffectDelegate(), _timing = AppTiming(delayRead = 0, delayWrite = 0),
      )
      store.put("car", vm)
      return vm
   }

   private fun uri(name: String) = Uri.parse("content://images/$name")
   private fun addCamera(vm: CarViewModel, path: String = photo) {
      storage.files += path // CameraPickerHandler has already created/confirmed this file.
      vm.onIntent(CarIntent.CameraImageTaken(path))
   }

   @Test fun import_blocksSaveCancelAndOverlap_butAllowsTextInput() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<String>>()
      storage.copy = { gate.await() }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("a"))))
         assertTrue(vm.stateFlow.value.isEditingImages)
         vm.onIntent(CarIntent.Save)
         vm.onIntent(CarIntent.Cancel) // Also used by Back.
         vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("b"))))
         vm.onIntent(CarIntent.ImageRemoved(original))
         vm.onIntent(CarIntent.ModelChanged("Fiesta"))
         runCurrent()
         assertEquals(listOf(uri("a")), storage.copied)
         assertEquals("Fiesta", vm.stateFlow.value.form.model)
         assertEquals(listOf(original), vm.stateFlow.value.form.imagePaths)
         assertTrue(writes.isEmpty())
         expectNoEvents()
         gate.complete(Result.success(photo))
         runCurrent()
         assertFalse(vm.stateFlow.value.isEditingImages)
         assertEquals(listOf(original, photo), vm.stateFlow.value.form.imagePaths)
         assertEquals(vm.stateFlow.value.form.imagePaths, edit.add(emptyList()))
         // Cancel was not queued; only this new request leaves the editor.
         expectNoEvents()
         vm.onIntent(CarIntent.Cancel)
         runCurrent()
         assertEquals(CarEffect.NavigateBack(BackReason.Cancel), awaitItem())
         assertTrue(original in storage.files)
         assertFalse(photo in storage.files)
      }
   }

   @Test fun removal_keepsGuardUntilDeletionAndFormUpdateFinish() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      addCamera(vm)
      val gate = CompletableDeferred<Result<Unit>>()
      storage.beforeDelete = { gate.await() }
      vm.onIntent(CarIntent.ImageRemoved(photo))
      assertTrue(vm.stateFlow.value.isEditingImages)
      assertEquals(listOf(original, photo), vm.stateFlow.value.form.imagePaths)
      vm.onIntent(CarIntent.Save)
      vm.onIntent(CarIntent.Cancel)
      runCurrent()
      assertTrue(writes.isEmpty())
      gate.complete(Result.success(Unit))
      runCurrent()
      assertFalse(vm.stateFlow.value.isEditingImages)
      assertEquals(listOf(original), vm.stateFlow.value.form.imagePaths)
      assertFalse(photo in storage.files)
      vm.onIntent(CarIntent.Save)
      advanceUntilIdle()
      assertEquals(listOf(original), writes.single().imagePaths)
   }

   @Test fun save_freezesSession_rejectsLateResults_andCommitsOnlyAfterPersistence() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      addCamera(vm)
      vm.onIntent(CarIntent.ImageRemoved(original))
      val gate = CompletableDeferred<Result<Unit>>()
      write = { gate.await() }
      vm.onIntent(CarIntent.Save)
      assertTrue(vm.stateFlow.value.isSaving)
      vm.onIntent(CarIntent.ImageRemoved(photo))
      vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("late"))))
      val late = "/images/late-camera.jpg"
      addCamera(vm, late)
      vm.onIntent(CarIntent.CameraImageTaken(original)) // Original removed from form, still persistent.
      vm.onIntent(CarIntent.CameraImageTaken(photo)) // Already adopted.
      runCurrent()
      assertTrue(storage.copied.isEmpty())
      assertFalse(late in storage.files)
      assertTrue(photo in storage.files)
      assertTrue(original in storage.files)
      assertEquals(listOf(photo), vm.stateFlow.value.form.imagePaths)
      assertEquals(listOf(photo), edit.add(emptyList()))
      assertEquals(listOf(photo), writes.single().imagePaths)
      gate.complete(Result.success(Unit))
      runCurrent()
      assertFalse(original in storage.files)
      assertTrue(photo in storage.files)
      assertTrue(storage.events.indexOf("persisted") < storage.events.indexOf("delete:$original"))
   }

   @Test fun closing_rejectsGalleryAndCleansOnlyFreshCameraFiles() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      vm.onIntent(CarIntent.Cancel)
      val late = "/images/late.jpg"
      addCamera(vm, late)
      vm.onIntent(CarIntent.CameraImageTaken(original))
      vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("late"))))
      advanceUntilIdle()
      assertTrue(storage.copied.isEmpty())
      assertFalse(late in storage.files)
      assertTrue(original in storage.files)
      assertEquals(listOf(original), vm.stateFlow.value.form.imagePaths)
      assertFalse(vm.stateFlow.value.isEditingImages)
   }

   @Test fun loadingLoadFailureAndNotFound_rejectImageOperations() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<Car?>>()
      backing.find = { gate.await() }
      val vm = create()
      runCurrent()
      fun reject(path: String) {
         addCamera(vm, path)
         vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("blocked"))))
         vm.onIntent(CarIntent.ImageRemoved(original))
      }
      reject("/images/loading.jpg")
      gate.complete(Result.failure(IllegalStateException("offline")))
      runCurrent()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.Failed)
      reject("/images/failed.jpg")
      backing.find = { Result.success(null) }
      vm.onIntent(CarIntent.RetryLoad)
      runCurrent()
      assertTrue(vm.stateFlow.value.loadFailure is CarLoadFailure.NotFound)
      reject("/images/not-found.jpg")
      advanceUntilIdle()
      assertTrue(storage.copied.isEmpty())
      assertEquals(setOf(original), storage.files)
      assertFalse(vm.stateFlow.value.isEditingImages)
   }

   @Test fun partialCopyFailure_keepsSuccessfulImagesAndReleasesGuard() = runTest(dispatcherRule.testDispatcher) {
      storage.copy = {
         if (it == uri("bad")) Result.failure(IllegalStateException("cannot read"))
         else Result.success("/images/${it.lastPathSegment}.jpg")
      }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("a"), uri("bad"), uri("b"))))
         runCurrent()
         assertFalse(vm.stateFlow.value.isEditingImages)
         assertEquals(listOf(original, "/images/a.jpg", "/images/b.jpg"), vm.stateFlow.value.form.imagePaths)
         assertTrue(awaitItem() is CarEffect.ShowError)
         vm.onIntent(CarIntent.Save)
         runCurrent()
         assertEquals(vm.stateFlow.value.form.imagePaths, writes.single().imagePaths)
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
      }
   }

   @Test fun immediateCancellation_stillReleasesSynchronousGuard() = runTest(dispatcherRule.testDispatcher) {
      storage.copy = { awaitCancellation() }
      val vm = create()
      advanceUntilIdle()
      vm.effects.test {
         vm.onIntent(CarIntent.GalleryImagesSelected(listOf(uri("a"))))
         assertTrue(vm.stateFlow.value.isEditingImages)
         store.clear() // No dispatcher turn between accepting the intent and cancellation.
         runCurrent()
         assertFalse(vm.stateFlow.value.isEditingImages)
         assertEquals(setOf(original), storage.files)
         expectNoEvents()
      }
   }

   @Test fun failedSave_keepsSessionForRetry_andCommitFollowsSuccessfulWrite() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      addCamera(vm)
      vm.onIntent(CarIntent.ImageRemoved(original))
      write = { Result.failure(IllegalStateException("write failed")) }
      vm.effects.test {
         vm.onIntent(CarIntent.Save)
         runCurrent()
         assertTrue(awaitItem() is CarEffect.ShowError)
         assertTrue(original in storage.files)
         assertEquals(listOf(photo), edit.add(emptyList()))
         write = { Result.success(Unit) }
         vm.onIntent(CarIntent.Save)
         runCurrent()
         assertTrue(awaitItem() is CarEffect.ShowMessage)
         assertEquals(CarEffect.NavigateBack(BackReason.Save), awaitItem())
         assertFalse(original in storage.files)
         assertTrue(photo in storage.files)
         assertEquals(2, writes.size)
      }
   }

   @Test fun failedSave_thenCancelDiscardsOnlySessionImages() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      advanceUntilIdle()
      addCamera(vm)
      vm.onIntent(CarIntent.ImageRemoved(original))
      write = { Result.failure(IllegalStateException("write failed")) }
      vm.onIntent(CarIntent.Save)
      runCurrent()
      vm.onIntent(CarIntent.Cancel)
      runCurrent()
      assertEquals(setOf(original), storage.files)
      assertEquals(listOf(original), edit.add(emptyList()))
      assertFalse("persisted" in storage.events)
   }

   @Test fun create_persistsExactlyTheAdoptedSession() = runTest(dispatcherRule.testDispatcher) {
      storage.files.clear()
      val vm = create(null)
      advanceUntilIdle()
      vm.onIntent(CarIntent.ManufacturerChanged("Ford"))
      vm.onIntent(CarIntent.ModelChanged("Focus"))
      vm.onIntent(CarIntent.SellerChanged("p1"))
      addCamera(vm)
      vm.onIntent(CarIntent.Save)
      runCurrent()
      assertEquals(listOf(photo), writes.single().imagePaths)
      assertEquals(setOf(photo), storage.files)
      assertEquals(listOf(photo), edit.add(emptyList()))
   }
}
