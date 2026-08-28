package de.rogallab.mobile.ui.navigation

import app.cash.turbine.test
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   // Creates the ViewModel with the dependencies required by A3_04.
   private fun createViewModel() =
      PeopleViewModel(
         _repository = FakePersonRepository(),
         _stringProvider = FakeStringProvider(),
         _effectDelegate = EffectDelegate(),
      )

   @Test
   fun create_emitsNavigateToWithNullPersonId() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.effects.test {
         viewModel.onIntent(PeopleIntent.Create)
         advanceUntilIdle()

         val effect = awaitItem() as PeopleEffect.NavigateTo
         assertNull(effect.personId)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun detail_emitsNavigateToWithPersonId() = runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.effects.test {
         viewModel.onIntent(PeopleIntent.Detail("p42"))
         advanceUntilIdle()

         val effect = awaitItem() as PeopleEffect.NavigateTo
         assertEquals("p42", effect.personId)
         cancelAndIgnoreRemainingEvents()
      }
   }
}
