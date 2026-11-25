package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PersonLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val strings = FakeStringProvider()
   private val validator = PersonValidator(ApplicationProvider.getApplicationContext())

   private fun createViewModel(id: String, repository: IPersonRepository): PersonViewModel {
      val imageFileStorage = FakeImageFileStorage()
      val imageEdit = ImageEdit(imageFileStorage)
      return PersonViewModel(
         personId = id,
         _repository = repository,
         _stringProvider = strings,
         _validator = validator,
         _images = ImageEditDelegate(imageEdit, imageFileStorage, ImageSelectionMode.REPLACE, 1),
         _personUcCreate = PersonUcCreate(repository, imageEdit),
         _personUcUpdate = PersonUcUpdate(repository, imageEdit),
         _timing = AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10),
         _effectDelegate = EffectDelegate(),
      )
   }

   @Test
   fun successfulLoad_showsPersonWithoutFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val repository = FakePersonRepository().apply { findResult = Result.success(person) }
      val viewModel = createViewModel("person-1", repository)

      viewModel.effects.test {
         assertTrue(viewModel.stateFlow.value.isLoading)
         advanceUntilIdle()

         assertEquals(person, viewModel.stateFlow.value.person)
         assertNull(viewModel.stateFlow.value.loadFailure)
         assertFalse(viewModel.stateFlow.value.isLoading)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun missingPerson_staysVisibleAndCannotBeSaved() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository()
      val viewModel = createViewModel("missing", repository)
      advanceUntilIdle()

      assertEquals(
         strings.getString(R.string.error_person_not_found),
         (viewModel.stateFlow.value.loadFailure as PersonLoadFailure.NotFound).message,
      )
      assertFalse(viewModel.stateFlow.value.isLoading)

      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
      viewModel.onIntent(PersonIntent.Save)
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()
      assertTrue(repository.updated.isEmpty())
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.NotFound)
   }

   @Test
   fun repositoryFailure_staysVisibleAndCanBeRetried() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         findResult = Result.failure(IllegalStateException("offline"))
      }
      val viewModel = createViewModel("person-1", repository)
      advanceUntilIdle()

      assertEquals(
         strings.getString(R.string.error_person_load),
         (viewModel.stateFlow.value.loadFailure as PersonLoadFailure.Failed).message,
      )
      assertFalse(viewModel.stateFlow.value.isLoading)

      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()
      assertTrue(repository.updated.isEmpty())
      assertFalse(viewModel.stateFlow.value.isSaving)
   }

   @Test
   fun resultFailure_becomesPersistentFailure() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository().apply {
            findResult = Result.failure(IllegalStateException("load failure"))
         }
         val viewModel = createViewModel("person-1", repository)
         advanceUntilIdle()

         assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.Failed)
         assertFalse(viewModel.stateFlow.value.isLoading)
      }

   @Test
   fun retry_clearsFailureAndPreventsParallelLoads() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      var findCalls = 0
      val repository = object : IPersonRepository by FakePersonRepository() {
         override suspend fun findById(id: String): Result<Person?> {
            findCalls++
            return if (findCalls == 1) Result.failure(IllegalStateException("offline"))
            else Result.success(person)
         }
      }
      val viewModel = createViewModel("person-1", repository)
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.loadFailure is PersonLoadFailure.Failed)

      viewModel.onIntent(PersonIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      assertNull(viewModel.stateFlow.value.loadFailure)
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()

      assertEquals(2, findCalls)
      assertEquals(person, viewModel.stateFlow.value.person)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun cancelledLoad_propagatesWithoutErrorEffectOrLoadFailure() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val repository = object : IPersonRepository by FakePersonRepository() {
               override suspend fun findById(id: String): Result<Person?> {
                  val cancellation = CancellationException("cancelled load")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val store = ViewModelStore()
            val viewModel = createViewModel("p1", repository).also { store.put("person", it) }
            val loading = viewModel.viewModelScope.coroutineContext[
               kotlinx.coroutines.Job
            ]!!.children.single()
            try {
               viewModel.effects.test {
                  advanceUntilIdle()
                  assertTrue(loading.isCancelled)
                  assertNull(viewModel.stateFlow.value.loadFailure)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun lateLoadResult_afterAcceptedCancel_doesNotChangeTheState() =
      runTest(mainDispatcherRule.testDispatcher) {
         val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
         for (outcome in listOf<Result<Person?>>(
            Result.success(person), Result.failure(IllegalStateException("late failure"))
         )) {
            val pending = CompletableDeferred<Result<Person?>>()
            val repository = object : IPersonRepository by FakePersonRepository() {
               override suspend fun findById(id: String): Result<Person?> = pending.await()
            }
            val store = ViewModelStore()
            val viewModel = createViewModel("p1", repository).also { store.put("person", it) }
            try {
               viewModel.effects.test {
                  advanceUntilIdle()
                  viewModel.onIntent(PersonIntent.Cancel)
                  runCurrent()
                  assertEquals(
                     BackReason.Cancel,
                     (awaitItem() as PersonEffect.NavigateBack).reason,
                  )
                  val state = viewModel.stateFlow.value
                  pending.complete(outcome)
                  advanceUntilIdle()
                  assertEquals(state, viewModel.stateFlow.value)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun saveDuringLoad_doesNotWrite() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         findResult = Result.success(Person(firstName = "Ada", lastName = "Lovelace", id = "person-1"))
      }
      val viewModel = createViewModel("person-1", repository)
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
      viewModel.onIntent(PersonIntent.Save)
      advanceUntilIdle()

      assertTrue(repository.updated.isEmpty())
      assertFalse(viewModel.stateFlow.value.isSaving)
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests trennen erfolgreiche Ladevorgänge, dauerhafte Fehlerzustände
 *   und den erneuten Ladeversuch von einmaligen UI-Effects.
 */
