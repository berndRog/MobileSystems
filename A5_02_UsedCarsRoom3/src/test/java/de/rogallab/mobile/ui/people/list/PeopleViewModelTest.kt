package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.FakePersonRepository
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PeopleViewModelTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   private val ada = Person(id = "1", firstName = "Ada", lastName = "Lovelace")
   private val grace = Person(id = "2", firstName = "Grace", lastName = "Hopper")
   private val alan = Person(id = "3", firstName = "Alan", lastName = "Turing")

   @Test
   fun removeAndRestore_keepOriginalPosition() {
      val repository = FakePersonRepository(listOf(ada, grace, alan))
      val viewModel = PeopleViewModel(repository)

      viewModel.onIntent(
         PeopleIntent.Remove(
            person = grace,
            originalIndex = 1
         )
      )

      assertEquals(listOf(ada, alan), viewModel.state.value.people)

      viewModel.onIntent(
         PeopleIntent.Restore(
            person = grace,
            originalIndex = 1
         )
      )

      assertEquals(listOf(ada, grace, alan), viewModel.state.value.people)
      assertEquals(grace.id, viewModel.state.value.restoredPersonId)

      viewModel.onIntent(PeopleIntent.Restored)
      assertNull(viewModel.state.value.restoredPersonId)
   }

   @Test
   fun staleRoomSnapshot_doesNotReinsertVisuallyRemovedPerson() {
      val repository = FakePersonRepository(listOf(ada, grace, alan))
      val viewModel = PeopleViewModel(repository)

      viewModel.onIntent(
         PeopleIntent.Remove(
            person = grace,
            originalIndex = 1
         )
      )

      repository.emitDatabaseSnapshot(listOf(alan, grace, ada))

      assertEquals(listOf(alan, ada), viewModel.state.value.people)
   }
   @Test
   fun loadFailure_emitsSnackbarEvent() = runTest {
      val repository = FakePersonRepository(listOf(ada))
      val viewModel = PeopleViewModel(repository)

      viewModel.events.test {
         repository.emitObservationFailure()

         val event = awaitItem() as PeopleEfect.ShowSnackbar
         assertEquals(
            UiText.Resource(R.string.error_people_load),
            event.message,
         )

         cancelAndIgnoreRemainingEvents()
      }
   }

}

