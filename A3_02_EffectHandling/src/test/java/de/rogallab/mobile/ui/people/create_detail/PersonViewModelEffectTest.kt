package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
import org.robolectric.shadows.ShadowLog

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class PersonViewModelEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val repository = FakePersonRepository()
   private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
   private val validator = PersonValidator(context)
   private val stringProvider = StringProvider(context)

   @Test
   fun save_invalidPersonEmitsErrorString() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.FirstNameChange("A"))
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()

         val effect = awaitItem() as PersonEffect.ShowError
         assertTrue(effect.message.isNotEmpty())
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun save_successEmitsMessageOnly() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()

         val message = awaitItem() as PersonEffect.ShowMessage
         assertEquals(
            stringProvider.getString(R.string.message_person_saved, "Ada Lovelace"),
            message.message,
         )
         assertEquals(1, repository.created.size)
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun save_repositoryFailureEmitsErrorWithoutNavigateBack() = runTest(mainDispatcherRule.testDispatcher) {
      repository.createResult = Result.failure(IllegalStateException("write failed"))
      Alog.set(useAndroidLog = true)
      ShadowLog.clear()
      val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()

         val error = awaitItem() as PersonEffect.ShowError
         assertEquals(stringProvider.getString(R.string.error_person_save), error.message)
         assertFalse(viewModel.stateFlow.value.isSaving)
         assertEquals(1, ShadowLog.getLogsForTag("<-PersonViewModel").count { it.type == Log.ERROR })
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun successfulLoadStoresPersonWithoutTestMessage() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      repository.findResult = Result.success(person)
      val viewModel = PersonViewModel("person-1", repository, stringProvider, validator, EffectDelegate())

      viewModel.effects.test {
         advanceUntilIdle()
         assertEquals(person, viewModel.stateFlow.value.person)
         assertNull(viewModel.stateFlow.value.loadFailure)
         assertFalse(viewModel.stateFlow.value.isLoading)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun missingExistingPersonCannotBeSavedButCanRetry() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.success(null)

      val viewModel = PersonViewModel("missing", repository, stringProvider, validator, EffectDelegate())

      viewModel.effects.test {
         advanceUntilIdle()
         assertEquals(PersonLoadFailure.NotFound(stringProvider.getString(R.string.error_person_not_found)),
            viewModel.stateFlow.value.loadFailure)
         viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
         viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
         viewModel.onIntent(PersonIntent.Save)
         advanceUntilIdle()
         assertTrue(repository.updated.isEmpty())
         val person = Person(firstName = "Ada", lastName = "Lovelace", id = "missing")
         repository.findResult = Result.success(person)
         viewModel.onIntent(PersonIntent.RetryLoad)
         advanceUntilIdle()
         assertEquals(person, viewModel.stateFlow.value.person)
         assertNull(viewModel.stateFlow.value.loadFailure)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun repositoryLoadFailureCanBeRetried() = runTest(mainDispatcherRule.testDispatcher) {
      repository.findResult = Result.failure(IllegalStateException("offline"))
      val viewModel = PersonViewModel("person-1", repository, stringProvider, validator, EffectDelegate())
      advanceUntilIdle()

      assertEquals(PersonLoadFailure.Failed(stringProvider.getString(R.string.error_person_load)),
         viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)

      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      repository.findResult = Result.success(person)
      viewModel.onIntent(PersonIntent.RetryLoad)
      assertTrue(viewModel.stateFlow.value.isLoading)
      assertNull(viewModel.stateFlow.value.loadFailure)
      viewModel.onIntent(PersonIntent.RetryLoad)
      advanceUntilIdle()

      assertEquals(person, viewModel.stateFlow.value.person)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun resultLoadFailureBecomesPersistentFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         findResult = Result.failure(IllegalStateException("load failed"))
      }
      val viewModel = PersonViewModel("person-1", repository,
         stringProvider, validator, EffectDelegate())
      advanceUntilIdle()

      assertEquals(PersonLoadFailure.Failed(stringProvider.getString(R.string.error_person_load)),
         viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun saveAndCancelAreBlockedUntilWriteCompletes() = runTest(mainDispatcherRule.testDispatcher) {
      val pending = CompletableDeferred<Result<Unit>>()
      var createCalls = 0
      val source = FakePersonRepository()
      val waitingRepository = object : IPersonRepository by source {
         override suspend fun create(person: Person): Result<Unit> {
            createCalls++
            return pending.await()
         }
      }
      val viewModel = PersonViewModel(null, waitingRepository,
         stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         viewModel.onIntent(PersonIntent.Save)
         viewModel.onIntent(PersonIntent.Cancel)
         assertTrue(viewModel.stateFlow.value.isSaving)
         assertEquals("Ada", viewModel.stateFlow.value.person.firstName)
         runCurrent()
         assertEquals(1, createCalls)

         pending.complete(Result.success(Unit))
         advanceUntilIdle()
         assertTrue(awaitItem() is PersonEffect.ShowMessage)
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun resultSaveFailureEmitsErrorAndResetsSaving() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         createResult = Result.failure(IllegalStateException("write failed"))
      }
      val viewModel = PersonViewModel(null, repository,
         stringProvider, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         assertTrue(viewModel.stateFlow.value.isSaving)
         advanceUntilIdle()

         val error = awaitItem() as PersonEffect.ShowError
         assertEquals(stringProvider.getString(R.string.error_person_save), error.message)
         assertFalse(viewModel.stateFlow.value.isSaving)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }
   @Test
   fun cancelledSave_releasesGuardWithoutError_whetherThrownOrReturned() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val source = object : IPersonRepository by repository {
               override suspend fun create(person: Person): Result<Unit> {
                  val cancellation = CancellationException("write cancelled")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = PersonViewModel(null, source, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            try {
               viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
               viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
               viewModel.effects.test {
                  viewModel.onIntent(PersonIntent.Save)
                  assertTrue(viewModel.stateFlow.value.isSaving)
                  advanceUntilIdle()
                  assertFalse(viewModel.stateFlow.value.isSaving)
                  expectNoEvents()
               }
            } finally {
               store.clear()
            }
         }
      }

   @Test
   fun clearedScope_releasesSaveGuardEvenBeforeTheBodyStarts() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (clearBeforeSave in listOf(false, true)) {
            val viewModel = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
            viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))
            viewModel.effects.test {
               if (clearBeforeSave) store.clear()
               viewModel.onIntent(PersonIntent.Save)
               store.clear()
               advanceUntilIdle()
               assertFalse(viewModel.stateFlow.value.isSaving)
               assertTrue(repository.created.isEmpty())
               expectNoEvents()
            }
         }
      }

   @Test
   fun cancelledLoad_isNotAStoredFailureOrMessage_whetherThrownOrReturned() =
      runTest(mainDispatcherRule.testDispatcher) {
         for (throwDirectly in listOf(false, true)) {
            val source = object : IPersonRepository by repository {
               override suspend fun findById(id: String): Result<Person?> {
                  val cancellation = CancellationException("load cancelled")
                  if (throwDirectly) throw cancellation
                  return Result.failure(cancellation)
               }
            }
            val viewModel = PersonViewModel("p1", source, stringProvider, validator, EffectDelegate())
            val store = ViewModelStore().also { it.put("person", viewModel) }
            val loading = viewModel.viewModelScope.coroutineContext[Job]!!.children.single()
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
   fun differentEditors_keepTheirEffectChannelsSeparate() =
      runTest(mainDispatcherRule.testDispatcher) {
         val first = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
         val second = PersonViewModel(null, repository, stringProvider, validator, EffectDelegate())
         val store = ViewModelStore().also {
            it.put("first", first)
            it.put("second", second)
         }
         try {
            first.onIntent(PersonIntent.FirstNameChange("Ada"))
            first.onIntent(PersonIntent.LastNameChange("Lovelace"))
            first.effects.test {
               val firstEvents = this
               second.effects.test {
                  first.onIntent(PersonIntent.Save)
                  advanceUntilIdle()
                  assertTrue(firstEvents.awaitItem() is PersonEffect.ShowMessage)
                  expectNoEvents()
                  second.onIntent(PersonIntent.Save)
                  advanceUntilIdle()
                  assertTrue(awaitItem() is PersonEffect.ShowError)
                  firstEvents.expectNoEvents()
               }
            }
         } finally {
            store.clear()
         }
      }

}

/*
 * Didaktik und Lernziele
 *
 * - Ladefehler bleiben im State, während Save und Validierung einmalige Effects senden.
 * - Ein laufender Save blockiert weitere Schreibversuche und Cancel.
 */
