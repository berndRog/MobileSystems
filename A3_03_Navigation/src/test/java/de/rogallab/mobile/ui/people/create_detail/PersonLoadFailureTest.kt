package de.rogallab.mobile.ui.people.create_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
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

   @Test
   fun successfulLoad_showsPersonWithoutFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      val repository = FakePersonRepository().apply { findResult = Result.success(person) }
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())

      assertTrue(viewModel.stateFlow.value.isLoading)
      advanceUntilIdle()

      assertEquals(person, viewModel.stateFlow.value.person)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun missingPerson_staysVisibleAndCannotBeSaved() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository()
      val viewModel = PersonViewModel("missing", repository, strings, validator, EffectDelegate())
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
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())
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
   fun thrownRepositoryException_becomesPersistentFailure() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = object : IPersonRepository by FakePersonRepository() {
            override suspend fun findById(id: String): Result<Person?> {
               throw IllegalStateException("unexpected load failure")
            }
         }
         val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())
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
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())
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
   fun saveDuringLoad_doesNotWrite() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         findResult = Result.success(Person(firstName = "Ada", lastName = "Lovelace", id = "person-1"))
      }
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())
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
