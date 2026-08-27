package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.removal.VisualRemovalDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelSwipeTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val ada = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")
   private val grace = Person(firstName = "Grace", lastName = "Hopper", id = "p2")
   private val stringProvider = FakeStringProvider()

   // Creates a ViewModel with its own temporary removal state for every test.
   private fun createViewModel(repository: FakePersonRepository) =
      PeopleViewModel(
         _repository = repository,
         _stringProvider = stringProvider,
         _visualRemoval = VisualRemovalDelegate<Person> { person -> person.id },
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun remove_hidesPersonButDoesNotTouchRepository() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace))
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.effects.test {
         viewModel.onIntent(PeopleIntent.Remove(ada))
         advanceUntilIdle()

         assertEquals(listOf(grace), viewModel.stateFlow.value.people)
         assertTrue(repository.removed.isEmpty())

         val effect = awaitItem() as PeopleEffect.ShowUndo
         assertEquals("p1", effect.personId)
         assertEquals(
            stringProvider.getString(R.string.message_person_removed, "Ada Lovelace"),
            effect.message,
         )
         assertEquals(stringProvider.getString(R.string.action_undo), effect.actionLabel)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun undoRemove_restoresPersonWithoutRepositoryDelete() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace))
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.onIntent(PeopleIntent.Remove(ada))
      advanceUntilIdle()
      viewModel.onIntent(PeopleIntent.UndoRemove("p1"))
      advanceUntilIdle()

      assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
      assertTrue(repository.removed.isEmpty())
   }

   @Test
   fun commitRemove_deletesFromRepositoryOnlyAfterUndoWindow() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace))
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.onIntent(PeopleIntent.Remove(ada))
      advanceUntilIdle()
      assertTrue(repository.removed.isEmpty())

      viewModel.onIntent(PeopleIntent.CommitRemove("p1"))
      advanceUntilIdle()

      assertEquals(listOf(ada), repository.removed)
      assertEquals(listOf(grace), viewModel.stateFlow.value.people)
   }

   @Test
   fun failedCommit_restoresPersonAndEmitsErrorString() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace)).apply {
         removeResult = Result.failure(IllegalStateException("delete failed"))
      }
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.onIntent(PeopleIntent.Remove(ada))
      advanceUntilIdle()

      viewModel.effects.test {
         // Ignore the already buffered Undo effect from the visual removal.
         assertTrue(awaitItem() is PeopleEffect.ShowUndo)

         viewModel.onIntent(PeopleIntent.CommitRemove("p1"))
         advanceUntilIdle()

         val error = awaitItem() as PeopleEffect.ShowError
         assertEquals(stringProvider.getString(R.string.error_person_remove), error.message)
         assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun removingSamePersonTwice_createsOnlyOnePendingUndo() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace))
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.effects.test {
         viewModel.onIntent(PeopleIntent.Remove(ada))
         viewModel.onIntent(PeopleIntent.Remove(ada))
         advanceUntilIdle()

         assertTrue(awaitItem() is PeopleEffect.ShowUndo)
         expectNoEvents()
         assertEquals(listOf(grace), viewModel.stateFlow.value.people)
         assertTrue(repository.removed.isEmpty())
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun commitUnknownPerson_doesNotTouchRepository() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository(listOf(ada, grace))
      val viewModel = createViewModel(repository)
      advanceUntilIdle()

      viewModel.onIntent(PeopleIntent.CommitRemove("unknown"))
      advanceUntilIdle()

      assertTrue(repository.removed.isEmpty())
      assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
   }
}
