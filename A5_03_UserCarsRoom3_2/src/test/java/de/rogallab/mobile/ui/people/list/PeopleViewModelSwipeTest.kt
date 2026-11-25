package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

   private val adaItem = PersonListItem(ada.id, "Ada Lovelace", null, null, ada.imagePath)
   private val graceItem = PersonListItem(grace.id, "Grace Hopper", null, null, grace.imagePath)

   private class FakePersonReadModel(items: List<PersonListItem> = emptyList()) : IPersonReadModel {
      val itemsFlow = MutableStateFlow(Result.success(items))
      var observations = 0
         private set

      override fun observeSelectItems(): Flow<Result<List<PersonSelectItem>>> =
         flowOf(Result.success(emptyList()))

      override fun observeListItems(): Flow<Result<List<PersonListItem>>> {
         observations++
         return itemsFlow
      }
   }

   private fun createViewModel(
      repository: FakePersonRepository,
      readModel: FakePersonReadModel,
   ) = PeopleViewModel(
      _readModel = readModel,
      _personUcDelete = PersonUcDelete(repository, imageFileStorage),
      _stringProvider = stringProvider,
      _effectDelegate = EffectDelegate(),
   )

   @Test
   fun failedObservation_staysVisibleUntilRetry() = runTest(mainDispatcherRule.testDispatcher) {
      val readModel = FakePersonReadModel().apply {
         itemsFlow.value = Result.failure(IllegalStateException("offline"))
      }
      val viewModel = createViewModel(FakePersonRepository(), readModel)
      advanceUntilIdle()
      assertEquals(stringProvider.getString(R.string.error_people_observe),
         viewModel.stateFlow.value.loadFailure)
      readModel.itemsFlow.value = Result.success(listOf(adaItem))
      advanceUntilIdle()
      assertTrue(viewModel.stateFlow.value.listItems.isEmpty())
      viewModel.onIntent(PeopleIntent.RetryLoad)
      advanceUntilIdle()
      assertEquals(2, readModel.observations)
      assertEquals(listOf(adaItem), viewModel.stateFlow.value.listItems)
      assertNull(viewModel.stateFlow.value.loadFailure)
      assertFalse(viewModel.stateFlow.value.isLoading)
   }

   @Test
   fun requestRemove_dismissedConfirmationKeepsPerson() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace)).apply {
            findResult = Result.success(ada)
         }
         val readModel = FakePersonReadModel(listOf(adaItem, graceItem))
         val viewModel = createViewModel(repository, readModel)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(PeopleIntent.RequestRemove(ada.id))
            advanceUntilIdle()

            val effect = awaitItem() as PeopleEffect.ConfirmRemove
            assertEquals(ada.id, effect.personId)
            assertEquals(
               stringProvider.getString(
                  R.string.message_person_remove_confirm,
                  adaItem.fullName,
               ),
               effect.message,
            )
            assertEquals(
               stringProvider.getString(R.string.action_confirm),
               effect.actionLabel,
            )
            assertEquals(emptyList<Person>(), repository.removed)
            assertEquals(listOf(adaItem, graceItem), viewModel.stateFlow.value.listItems)

            cancelAndIgnoreRemainingEvents()
         }
         advanceUntilIdle()
         assertEquals(emptyList<Person>(), repository.removed)
         assertEquals(emptyList<String?>(), imageFileStorage.deletedPaths)
         assertEquals(listOf(adaItem, graceItem), viewModel.stateFlow.value.listItems)
      }

   @Test
   fun confirmRemove_deletesPersonAndUpdatesState() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace)).apply {
            findResult = Result.success(ada)
         }
         val readModel = FakePersonReadModel(listOf(adaItem, graceItem))
         val viewModel = createViewModel(repository, readModel)
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
         // Persistence does not change the projection until the read model emits it.
         assertEquals(listOf(adaItem, graceItem), viewModel.stateFlow.value.listItems)
         readModel.itemsFlow.value = Result.success(listOf(graceItem))
         advanceUntilIdle()
         assertEquals(listOf(graceItem), viewModel.stateFlow.value.listItems)
      }

   @Test
   fun failedConfirmRemove_emitsErrorAndKeepsPerson() =
      runTest(mainDispatcherRule.testDispatcher) {
         val repository = FakePersonRepository(listOf(ada, grace)).apply {
            findResult = Result.success(ada)
            removeResult = Result.failure(IllegalStateException("delete failed"))
         }
         val readModel = FakePersonReadModel(listOf(adaItem, graceItem))
         val viewModel = createViewModel(repository, readModel)
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
            assertEquals(listOf(adaItem, graceItem), viewModel.stateFlow.value.listItems)

            cancelAndIgnoreRemainingEvents()
         }
      }
}
