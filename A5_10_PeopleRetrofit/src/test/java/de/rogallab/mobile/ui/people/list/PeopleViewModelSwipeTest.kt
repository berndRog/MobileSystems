package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelSwipeTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")
   private val grace = Person(firstName = "Grace", lastName = "Hopper", id = "p2")
   private val stringProvider = FakeStringProvider()

   private fun createViewModel(repository: FakePersonRepository) =
      PeopleViewModel(
         _repository = repository,
         _stringProvider = stringProvider,
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun requestRemove_emitsConfirmationWithoutDeleting() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace))
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(PeopleIntent.RequestRemove(ada.id))
            advanceUntilIdle()

            val effect = awaitItem() as PeopleEffect.ConfirmRemove
            assertEquals(ada.id, effect.personId)
            assertEquals(
               stringProvider.getString(
                  R.string.message_person_remove_confirm,
                  ada.firstName,
                  ada.lastName,
               ),
               effect.message,
            )
            assertEquals(
               stringProvider.getString(R.string.action_confirm),
               effect.actionLabel,
            )
            assertEquals(emptyList<Person>(), repository.removed)
            assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)

            cancelAndIgnoreRemainingEvents()
         }
      }

   @Test
   fun confirmRemove_deletesPersonAndUpdatesState() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace))
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.onIntent(PeopleIntent.ConfirmRemove(ada.id))
         advanceUntilIdle()

         assertEquals(listOf(ada), repository.removed)
         assertEquals(listOf(grace), viewModel.stateFlow.value.people)
      }

   @Test
   fun failedConfirmRemove_emitsErrorAndKeepsPerson() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace)).apply {
            removeResult = Result.failure(IllegalStateException("delete failed"))
         }
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(PeopleIntent.ConfirmRemove(ada.id))
            advanceUntilIdle()

            val error = awaitItem() as PeopleEffect.ShowError
            assertEquals(
               stringProvider.getString(R.string.error_person_remove),
               error.message,
            )
            assertEquals(emptyList<Person>(), repository.removed)
            assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)

            cancelAndIgnoreRemainingEvents()
         }
      }
}
