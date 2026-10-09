package de.rogallab.mobile.ui.navigation

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.navigation.comp.deliverIfCurrent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
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
class NavigationDuringOperationTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val strings = FakeStringProvider()
   private val validator = PersonValidator(ApplicationProvider.getApplicationContext())

   @Test
   fun messageFromPoppedPerson_isNotDeliveredToSharedHost() {
      val personKey = PersonKey("person-1")
      val backStack = mutableListOf(PeopleKey, personKey)
      val delivered = mutableListOf<String>()

      deliverIfCurrent(backStack, personKey) { delivered += "before navigation" }
      backStack.removeLast()
      deliverIfCurrent(backStack, personKey) { delivered += "after navigation" }
      backStack += PersonKey("person-1")
      deliverIfCurrent(backStack, personKey) { delivered += "after reopening" }

      assertTrue(delivered == listOf("before navigation"))
   }

   @Test
   fun leavingPersonBeforeLoadFails_discardsLateFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val pending = CompletableDeferred<Result<Person?>>()
      val repository = object : IPersonRepository by FakePersonRepository() {
         override suspend fun findById(id: String): Result<Person?> = pending.await()
      }
      val viewModel = PersonViewModel("person-1", repository, strings, validator, EffectDelegate())

      viewModel.effects.test {
         advanceTimeBy(1000)
         runCurrent()
         assertTrue(viewModel.stateFlow.value.isLoading)

         viewModel.onScreenLeft()
         pending.complete(Result.failure(IllegalStateException("offline")))
         advanceUntilIdle()

         assertNull(viewModel.stateFlow.value.loadFailure)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun savingPreventsLeavingUntilTheResultArrives() = runTest(mainDispatcherRule.testDispatcher) {
      val pending = CompletableDeferred<Result<Unit>>()
      val repository = object : IPersonRepository by FakePersonRepository() {
         override suspend fun create(person: Person): Result<Unit> = pending.await()
      }
      val viewModel = PersonViewModel(null, repository, strings, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         runCurrent()
         assertTrue(viewModel.stateFlow.value.isSaving)
         assertFalse(viewModel.canLeaveScreen)
         viewModel.onIntent(PersonIntent.Cancel)
         expectNoEvents()

         pending.complete(Result.failure(IllegalStateException("offline")))
         advanceUntilIdle()

         assertFalse(viewModel.stateFlow.value.isSaving)
         assertTrue(viewModel.canLeaveScreen)
         assertTrue(awaitItem() is PersonEffect.ShowError)
         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun successfulSave_emitsMessageBeforeNavigation() = runTest(mainDispatcherRule.testDispatcher) {
      val pending = CompletableDeferred<Result<Unit>>()
      val repository = object : IPersonRepository by FakePersonRepository() {
         override suspend fun create(person: Person): Result<Unit> = pending.await()
      }
      val viewModel = PersonViewModel(null, repository, strings, validator, EffectDelegate())
      viewModel.onIntent(PersonIntent.FirstNameChange("Ada"))
      viewModel.onIntent(PersonIntent.LastNameChange("Lovelace"))

      viewModel.effects.test {
         viewModel.onIntent(PersonIntent.Save)
         runCurrent()
         pending.complete(Result.success(Unit))
         advanceUntilIdle()

         assertTrue(awaitItem() is PersonEffect.ShowMessage)
         assertEquals(BackReason.Save, (awaitItem() as PersonEffect.NavigateBack).reason)
         assertTrue(viewModel.canLeaveScreen)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun leavingPeopleBeforeRemoveFails_discardsLateFailure() = runTest(mainDispatcherRule.testDispatcher) {
      val pending = CompletableDeferred<Result<Unit>>()
      val repository = object : IPersonRepository by FakePersonRepository() {
         override suspend fun remove(person: Person): Result<Unit> = pending.await()
      }
      val viewModel = PeopleViewModel(repository, strings, EffectDelegate())

      viewModel.effects.test {
         val person = Person(firstName = "Ada", lastName = "Lovelace")
         viewModel.onIntent(PeopleIntent.Remove(person))
         runCurrent()

         viewModel.onScreenLeft()
         pending.complete(Result.failure(IllegalStateException("offline")))
         advanceUntilIdle()

         expectNoEvents()
         cancelAndIgnoreRemainingEvents()
      }
   }
}
