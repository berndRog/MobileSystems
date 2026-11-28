package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.images.*
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
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
class PersonViewModelCoordinationTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val store = ViewModelStore()
   private val original = "/images/original.jpg"
   private val replacement = "/images/new.jpg"
   private val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace", imagePath = original)
   private val findIds = mutableListOf<String>()
   private val writes = mutableListOf<Person>()
   private val events = mutableListOf<String>()
   private var find: suspend () -> Result<Person?> = { Result.success(person) }
   private var write: suspend () -> Result<Unit> = { Result.success(Unit) }
   private val repository = object : IPersonRepository by FakePersonRepository() {
      override suspend fun findById(id: String): Result<Person?> {
         findIds += id
         return find()
      }
      override suspend fun create(person: Person) = persist(person)
      override suspend fun update(person: Person) = persist(person)
      private suspend fun persist(value: Person): Result<Unit> {
         writes += value
         return write().onSuccess { events += "persisted" }
      }
   }
   // Only the delegate contract is faked here; file algorithms are tested in Shared.
   private class Images : IImageEditDelegate {
      var change: suspend () -> Result<ImageChange> = { Result.success(ImageChange()) }
      var imports = 0
      var discards = 0
      var discardOperation: suspend () -> Result<Unit> = { Result.success(Unit) }
      val rejected = mutableListOf<String>()
      var started = emptyList<String>()
      override fun start(originalPaths: List<String>) { started = originalPaths }
      override suspend fun importGallery(uris: List<Uri>): Result<ImageChange> {
         imports++
         return change()
      }
      override suspend fun acceptCamera(path: String) = change()
      override suspend fun rejectCamera(path: String): Result<Unit> {
         rejected += path
         return Result.success(Unit)
      }
      override suspend fun remove(path: String) = change()
      override suspend fun discard(): Result<Unit> {
         discards++
         return discardOperation()
      }
   }
   private val images = Images()
   private val session = FakeImageEdit()

   private fun create(
      id: String? = "p1",
      delegate: IImageEditDelegate = images,
      edit: IImageEdit = session,
      delayWrite: Long = 0,
   ): PersonViewModel = PersonViewModel(
      personId = id, _repository = repository,
      _stringProvider = FakeStringProvider(),
      _validator = PersonValidator(ApplicationProvider.getApplicationContext()),
      _images = delegate,
      _personUcCreate = PersonUcCreate(repository, edit, _imageFileStorage = de.rogallab.mobile.testing.FakeImageFileStorage()),
      _personUcUpdate = PersonUcUpdate(repository, edit, _imageFileStorage = de.rogallab.mobile.testing.FakeImageFileStorage()),
      _effectDelegate = EffectDelegate(),
      _timing = AppTiming(delayRead = 0, delayWrite = delayWrite),
   ).also { store.put("person", it) }

   @After fun tearDown() { store.clear() }

   @Test fun technicalFailureAndNotFound_blockSaveAndImages_untilSuccessfulRetry() =
      runTest(dispatcherRule.testDispatcher) {
         find = { Result.failure(IllegalStateException("offline")) }
         val vm = create()
         assertTrue(vm.stateFlow.value.isLoading)
         runCurrent()
         assertTrue(vm.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
         vm.onIntent(PersonIntent.FirstNameChange("Grace"))
         vm.onIntent(PersonIntent.LastNameChange("Hopper"))
         vm.onIntent(PersonIntent.Save)
         vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
         runCurrent()
         assertTrue(writes.isEmpty())
         assertEquals(0, images.imports)

         find = { Result.success(null) }
         vm.onIntent(PersonIntent.RetryLoad)
         runCurrent()
         assertTrue(vm.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
         vm.onIntent(PersonIntent.RetryLoad)
         vm.onIntent(PersonIntent.Save)
         runCurrent()
         assertEquals(2, findIds.size)
         assertTrue(writes.isEmpty())

         store.clear()
         find = { throw IllegalStateException("query failed") }
         val retryVm = create()
         runCurrent()
         assertTrue(retryVm.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
         find = { Result.success(person) }
         retryVm.onIntent(PersonIntent.RetryLoad)
         runCurrent()
         assertNull(retryVm.stateFlow.value.loadFailure)
         assertEquals(person.id, retryVm.stateFlow.value.form.id)
         assertEquals(listOf(original), images.started)
      }

   @Test fun repeatedRetries_startOnlyOneLoadForTheOriginalId() = runTest(dispatcherRule.testDispatcher) {
      find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      runCurrent()
      val gate = CompletableDeferred<Result<Person?>>()
      find = { gate.await() }
      repeat(10) { vm.onIntent(PersonIntent.RetryLoad) }
      assertTrue(vm.stateFlow.value.isLoading)
      runCurrent()
      assertEquals(listOf("p1", "p1"), findIds)
      gate.complete(Result.success(person))
      runCurrent()
      assertNull(vm.stateFlow.value.loadFailure)
   }

   @Test fun lateLoadOutcomes_afterCancellation_doNotChangeState() = runTest(dispatcherRule.testDispatcher) {
      for (outcome in listOf("success", "failure", "throw")) {
         find = {
            withContext(NonCancellable) { delay(100) }
            when (outcome) {
               "success" -> Result.success(person)
               "failure" -> Result.failure(IllegalStateException("late"))
               else -> throw IllegalStateException("late")
            }
         }
         val vm = create()
         runCurrent()
         val state = vm.stateFlow.value
         store.clear()
         advanceUntilIdle()
         assertEquals(state, vm.stateFlow.value)
      }
   }

   @Test fun cancellationResult_duringLoad_isNotAUserError() = runTest(dispatcherRule.testDispatcher) {
      find = { Result.failure(CancellationException("cancelled")) }
      val vm = create()
      vm.effects.test {
         runCurrent()
         assertTrue(vm.stateFlow.value.isLoading)
         assertNull(vm.stateFlow.value.loadFailure)
         expectNoEvents()
      }
   }

   @Test fun save_freezesSnapshot_blocksDuplicatesAndNavigation_untilEffectsAreHandled() =
      runTest(dispatcherRule.testDispatcher) {
         val vm = create()
         runCurrent()
         val snapshot = vm.stateFlow.value.form
         val gate = CompletableDeferred<Result<Unit>>()
         write = { gate.await() }
         vm.onIntent(PersonIntent.Save)
         assertTrue(vm.stateFlow.value.isSaving)
         vm.onIntent(PersonIntent.FirstNameChange("Changed"))
         vm.onIntent(PersonIntent.Save)
         vm.onIntent(PersonIntent.Cancel) // Also used by System Back.
         vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
         vm.onIntent(PersonIntent.CameraImageTaken("/images/late.jpg"))
         runCurrent()
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertEquals(1, writes.size)
         assertEquals(person.copy(email = null, phone = null), writes.single())
         assertEquals(0, images.imports)
         assertEquals(listOf("/images/late.jpg"), images.rejected)
         gate.complete(Result.success(Unit))
         runCurrent()
         assertFalse(vm.stateFlow.value.isSaving)
         vm.onIntent(PersonIntent.Save)
         vm.onIntent(PersonIntent.Cancel)
         vm.onIntent(PersonIntent.LastNameChange("Changed"))
         runCurrent()
         assertEquals(snapshot, vm.stateFlow.value.form)
         assertEquals(1, writes.size)
         assertEquals(0, images.discards)
         vm.effects.test {
            assertTrue(awaitItem() is PersonEffect.ShowMessage)
            assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
            expectNoEvents()
         }
      }

   @Test fun cancelledWrites_releaseSavingWithoutErrorOrNavigation() = runTest(dispatcherRule.testDispatcher) {
      for (throws in listOf(false, true)) {
         write = {
            if (throws) throw CancellationException("cancelled")
            Result.failure(CancellationException("cancelled"))
         }
         val vm = create()
         runCurrent()
         val snapshot = vm.stateFlow.value.form
         vm.effects.test {
            vm.onIntent(PersonIntent.Save)
            runCurrent()
            assertFalse(vm.stateFlow.value.isSaving)
            assertEquals(snapshot, vm.stateFlow.value.form)
            expectNoEvents()
         }
      }
   }

   @Test fun immediateCancellation_releasesSaveAndImageGuards() = runTest(dispatcherRule.testDispatcher) {
      val saving = create(delayWrite = 50)
      runCurrent()
      saving.onIntent(PersonIntent.Save)
      assertTrue(saving.stateFlow.value.isSaving)
      store.clear()
      runCurrent()
      assertFalse(saving.stateFlow.value.isSaving)
      assertTrue(writes.isEmpty())

      images.change = { awaitCancellation() }
      val importing = create()
      runCurrent()
      importing.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
      assertTrue(importing.stateFlow.value.isEditingImages)
      store.clear()
      runCurrent()
      assertFalse(importing.stateFlow.value.isEditingImages)
   }

   @Test fun acceptedCancel_discardsOnce_andBlocksQueuedActions() = runTest(dispatcherRule.testDispatcher) {
      val vm = create()
      runCurrent()
      vm.effects.test {
         vm.onIntent(PersonIntent.Cancel)
         vm.onIntent(PersonIntent.Save)
         vm.onIntent(PersonIntent.Cancel)
         vm.onIntent(PersonIntent.FirstNameChange("Changed"))
         runCurrent()
         assertEquals(1, images.discards)
         assertTrue(writes.isEmpty())
         assertEquals("Ada", vm.stateFlow.value.form.firstName)
         assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun imageImport_blocksSaveCancelAndOverlap_butAllowsTextInput() = runTest(dispatcherRule.testDispatcher) {
      val gate = CompletableDeferred<Result<ImageChange>>()
      images.change = { gate.await() }
      val vm = create()
      runCurrent()
      vm.effects.test {
         vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
         assertTrue(vm.stateFlow.value.isEditingImages)
         vm.onIntent(PersonIntent.Save)
         vm.onIntent(PersonIntent.Cancel)
         vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
         vm.onIntent(PersonIntent.RemoveImage(null))
         vm.onIntent(PersonIntent.FirstNameChange("Grace"))
         runCurrent()
         assertEquals(1, images.imports)
         assertEquals("Grace", vm.stateFlow.value.form.firstName)
         assertEquals(original, vm.stateFlow.value.form.imagePath)
         assertTrue(writes.isEmpty())
         expectNoEvents()
         gate.complete(Result.success(ImageChange(paths = listOf(replacement))))
         runCurrent()
         assertFalse(vm.stateFlow.value.isEditingImages)
         assertEquals(replacement, vm.stateFlow.value.form.imagePath)
         vm.onIntent(PersonIntent.Cancel)
         runCurrent()
         assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
      }
   }

   @Test fun imageChange_distinguishesUnchangedReplacementRemovalAndFailure() =
      runTest(dispatcherRule.testDispatcher) {
         val vm = create()
         runCurrent()
         vm.effects.test {
            images.change = { Result.success(ImageChange(paths = null)) }
            vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
            runCurrent()
            assertEquals(original, vm.stateFlow.value.form.imagePath)
            images.change = { Result.success(ImageChange(paths = listOf(replacement, "ignored"))) }
            vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
            runCurrent()
            assertEquals(replacement, vm.stateFlow.value.form.imagePath)
            images.change = { Result.failure(IllegalStateException("import failed")) }
            vm.onIntent(PersonIntent.GalleryImageSelected(Uri.EMPTY))
            runCurrent()
            assertEquals(replacement, vm.stateFlow.value.form.imagePath)
            assertTrue(awaitItem() is PersonEffect.ShowError)
            images.change = { Result.success(ImageChange(paths = emptyList())) }
            vm.onIntent(PersonIntent.RemoveImage(null))
            runCurrent()
            assertNull(vm.stateFlow.value.form.imagePath)
            assertFalse(vm.stateFlow.value.isEditingImages)
            expectNoEvents()
         }
      }

   @Test fun realDelegate_sharesSessionWithSave_andSupportsFailureThenRetryOrCancel() =
      runTest(dispatcherRule.testDispatcher) {
         for (retry in listOf(false, true)) {
            events.clear()
            val storage = object : IImageFileStorage by FakeImageFileStorage() {
               override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
                  events += "delete:$imagePath"
                  return Result.success(Unit)
               }
            }
            val edit = ImageEdit(storage)
            val vm = create(delegate = ImageEditDelegate(edit, storage, ImageSelectionMode.REPLACE, 1), edit = edit)
            runCurrent()
            vm.onIntent(PersonIntent.CameraImageTaken(replacement))
            runCurrent()
            assertEquals(replacement, vm.stateFlow.value.form.imagePath)
            assertEquals(listOf(replacement), edit.add(emptyList()))
            val snapshot = vm.stateFlow.value.form
            write = { throw IllegalStateException("write failed") }
            vm.effects.test {
               vm.onIntent(PersonIntent.Save)
               runCurrent()
               assertTrue(awaitItem() is PersonEffect.ShowError)
               assertFalse(vm.stateFlow.value.isSaving)
               assertEquals(snapshot, vm.stateFlow.value.form)
               assertTrue(events.isEmpty())
               if (retry) {
                  write = { Result.success(Unit) }
                  vm.onIntent(PersonIntent.Save)
                  runCurrent()
                  assertEquals(replacement, writes.last().imagePath)
                  assertEquals(listOf("persisted", "delete:$original"), events)
                  assertTrue(awaitItem() is PersonEffect.ShowMessage)
                  assertEquals(PersonEffect.NavigateBack(BackReason.Save), awaitItem())
               } else {
                  vm.onIntent(PersonIntent.Cancel)
                  runCurrent()
                  assertEquals(listOf("delete:$replacement"), events)
                  assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
               }
               expectNoEvents()
            }
         }
      }


   @Test fun lateLoadOutcomes_afterAcceptedCancel_doNotRestartTheSession() =
      runTest(dispatcherRule.testDispatcher) {
         for (outcome in listOf("success", "failure", "throw")) {
            val gate = CompletableDeferred<Unit>()
            find = {
               gate.await()
               when (outcome) {
                  "success" -> Result.success(person)
                  "failure" -> Result.failure(IllegalStateException("late"))
                  else -> throw IllegalStateException("late")
               }
            }
            val vm = create()
            runCurrent()
            vm.effects.test {
               vm.onIntent(PersonIntent.Cancel)
               runCurrent()
               assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
               val state = vm.stateFlow.value
               val started = images.started
               gate.complete(Unit)
               runCurrent()
               assertEquals(state, vm.stateFlow.value)
               assertEquals(started, images.started)
               expectNoEvents()
            }
            store.clear()
         }
      }

   @Test fun acceptedCancel_blocksRetryOfFailedLoad() = runTest(dispatcherRule.testDispatcher) {
      find = { Result.failure(IllegalStateException("offline")) }
      val vm = create()
      runCurrent()
      vm.effects.test {
         vm.onIntent(PersonIntent.Cancel)
         vm.onIntent(PersonIntent.RetryLoad)
         runCurrent()
         assertEquals(listOf("p1"), findIds)
         assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
         expectNoEvents()
      }
   }

   @Test fun failedDiscard_allowsOneRetry_andKeepsClosingActionsBlocked() =
      runTest(dispatcherRule.testDispatcher) {
         val vm = create()
         runCurrent()
         images.discardOperation = { Result.failure(IllegalStateException("cleanup failed")) }
         vm.effects.test {
            vm.onIntent(PersonIntent.Cancel)
            runCurrent()
            assertTrue(awaitItem() is PersonEffect.ShowError)
            vm.onIntent(PersonIntent.FirstNameChange("Grace"))
            assertEquals("Grace", vm.stateFlow.value.form.firstName)
            images.discardOperation = { Result.success(Unit) }
            vm.onIntent(PersonIntent.Cancel)
            vm.onIntent(PersonIntent.Cancel)
            vm.onIntent(PersonIntent.Save)
            runCurrent()
            assertEquals(2, images.discards)
            assertTrue(writes.isEmpty())
            assertEquals(PersonEffect.NavigateBack(BackReason.Cancel), awaitItem())
            expectNoEvents()
         }
      }

   @Test fun cancelledDiscard_hasNoErrorOrNavigation_whetherThrownOrReturned() =
      runTest(dispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            images.discardOperation = {
               val cancellation = CancellationException("discard cancelled")
               if (throwDirectly) throw cancellation
               Result.failure(cancellation)
            }
            val vm = create()
            runCurrent()
            vm.effects.test {
               vm.onIntent(PersonIntent.Cancel)
               runCurrent()
               expectNoEvents()
            }
            store.clear()
         }
      }

   @Test fun saveFailure_logsOnce_andCancellationDoesNotLogOrEmit() = runTest(dispatcherRule.testDispatcher) {
      de.rogallab.mobile.shared.domain.utilities.Alog.set(useAndroidLog = true)
      for (cancelled in listOf(false, true)) {
         org.robolectric.shadows.ShadowLog.clear()
         write = { Result.failure(if (cancelled) CancellationException("cancelled") else IllegalStateException("failed")) }
         val vm = create()
         runCurrent()
         vm.effects.test {
            vm.onIntent(PersonIntent.Save)
            runCurrent()
            val logs = org.robolectric.shadows.ShadowLog.getLogsForTag("<-PersonViewModel")
               .count { it.type == android.util.Log.ERROR }
            assertEquals(if (cancelled) 0 else 1, logs)
            assertFalse(vm.stateFlow.value.isSaving)
            if (!cancelled) assertTrue(awaitItem() is PersonEffect.ShowError)
            expectNoEvents()
         }
         store.clear()
      }
   }

   @Test fun rejectedCameraFailure_afterCancellation_doesNotPublishAnError() =
      runTest(dispatcherRule.testDispatcher) {
         val rejecting = CompletableDeferred<Result<Unit>>()
         val delegate = object : IImageEditDelegate by images {
            override suspend fun rejectCamera(path: String): Result<Unit> =
               withContext(NonCancellable) { rejecting.await() }
         }
         val editor = create(delegate = delegate)
         runCurrent()
         editor.onIntent(PersonIntent.Cancel)
         runCurrent()
         editor.effects.test {
            // Cancel already published navigation; only the late camera result is relevant.
            assertTrue(awaitItem() is PersonEffect.NavigateBack)
            editor.onIntent(PersonIntent.CameraImageTaken(replacement))
            runCurrent()
            store.clear()
            rejecting.complete(Result.failure(IllegalStateException("late cleanup")))
            runCurrent()
            expectNoEvents()
         }
      }
}
