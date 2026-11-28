package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleActionEffectTest {
   @get:Rule val dispatcherRule = MainDispatcherRule()
   private val ada = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
   private val strings = FakeStringProvider()

   @Test fun remove_emitsOneFormattedMessageOrOneError() = runTest(dispatcherRule.testDispatcher) {
      for (succeeds in listOf(true, false)) {
         val repository = FakePersonRepository(listOf(ada)).apply {
            if (!succeeds) removeResult = Result.failure(IllegalStateException("write failed"))
         }
         val vm = PeopleViewModel(repository, strings, EffectDelegate())
         val store = ViewModelStore().also { it.put("people", vm) }
         try {
            advanceUntilIdle()
            vm.effects.test {
               vm.onIntent(PeopleIntent.Remove(ada))
               advanceUntilIdle()
               val expected = if (succeeds) PeopleEffect.ShowMessage(
                  strings.getString(R.string.message_person_removed, ada.fullName)
               ) else PeopleEffect.ShowError(strings.getString(R.string.error_person_remove))
               assertEquals(expected, awaitItem())
               assertEquals(if (succeeds) listOf(ada) else emptyList<Person>(), repository.removed)
               expectNoEvents()
            }
         } finally {
            store.clear()
         }
      }
   }

   @Test fun cancelledRemove_hasNoMessageOrNavigation() = runTest(dispatcherRule.testDispatcher) {
      for (throwDirectly in listOf(false, true)) {
         val source = FakePersonRepository(listOf(ada))
         val repository = object : IPersonRepository by source {
            override suspend fun remove(person: Person): Result<Unit> {
               val cancellation = CancellationException("delete cancelled")
               if (throwDirectly) throw cancellation
               return Result.failure(cancellation)
            }
         }
         val vm = PeopleViewModel(repository, strings, EffectDelegate())
         val store = ViewModelStore().also { it.put("people", vm) }
         try {
            advanceUntilIdle()
            vm.effects.test {
               vm.onIntent(PeopleIntent.Remove(ada))
               advanceUntilIdle()
               assertEquals(listOf(ada), vm.stateFlow.value.people)
               assertTrue(source.removed.isEmpty())
               expectNoEvents()
            }
         } finally {
            store.clear()
         }
      }
   }
}
