package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelSwipeTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val ada = Person(
      firstName = "Ada",
      lastName = "Lovelace",
      imagePath = "/images/ada.jpg",
      id = "p1",
   )
   private val grace = Person(firstName = "Grace", lastName = "Hopper", id = "p2")
   private val stringProvider = FakeStringProvider()
   private val imageFileStorage = FakeImageFileStorage()

   private fun createViewModel(repository: FakePersonRepository) =
      PeopleViewModel(
         _repository = repository,
         _personUcDelete = PersonUcDelete(repository, imageFileStorage),
         _stringProvider = stringProvider,
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun failedObservation_staysVisibleUntilRetry() = runTest(mainDispatcherRule.testDispatcher) {
      val source = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("offline"))
      }
      var observations = 0
      val repository = object : IPersonRepository by source {
         override fun observeAll(): Flow<Result<List<Person>>> {
            observations++
            return source.observeAll()
         }
      }
      val viewModel = PeopleViewModel(
         repository, PersonUcDelete(repository, imageFileStorage), stringProvider, EffectDelegate()
      )
      advanceUntilIdle()
      assertEquals(stringProvider.getString(R.string.error_people_observe),
         viewModel.stateFlow.value.loadFailure)
      source.peopleFlow.value = Result.success(listOf(ada))
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.people.isEmpty())
      viewModel.onIntent(PeopleIntent.RetryLoad)
      advanceUntilIdle()
      assertEquals(2, observations)
      assertEquals(listOf(ada), viewModel.stateFlow.value.people)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun requestRemove_dismissedConfirmationKeepsPerson() =
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
         advanceUntilIdle()
         assertEquals(emptyList<Person>(), repository.removed)
         assertEquals(emptyList<String?>(), imageFileStorage.deletedPaths)
         assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)
      }

   @Test
   fun confirmRemove_deletesPersonAndUpdatesState() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace))
         val viewModel = createViewModel(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(PeopleIntent.RequestRemove(ada.id))
            advanceUntilIdle()
            val confirmation = awaitItem() as PeopleEffect.ConfirmRemove
            assertEquals(emptyList<Person>(), repository.removed)

            viewModel.onIntent(PeopleIntent.ConfirmRemove(confirmation.personId))
            advanceUntilIdle()
            assertTrue(awaitItem() is PeopleEffect.ShowMessage)
            cancelAndIgnoreRemainingEvents()
         }

         assertEquals(listOf(ada), repository.removed)
         assertEquals(listOf(ada.imagePath), imageFileStorage.deletedPaths)
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
            assertEquals(emptyList<String?>(), imageFileStorage.deletedPaths)
            assertEquals(listOf(ada, grace), viewModel.stateFlow.value.people)

            cancelAndIgnoreRemainingEvents()
         }
      }
}
