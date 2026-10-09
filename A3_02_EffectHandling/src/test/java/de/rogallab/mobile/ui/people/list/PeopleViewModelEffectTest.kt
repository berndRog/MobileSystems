package de.rogallab.mobile.ui.people.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelEffectTest {

   @get:Rule
   val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun observeFailure_remainsVisibleUntilSuccess() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakePersonRepository().apply {
         peopleFlow.value = Result.failure(IllegalStateException("read failed"))
      }
      val stringProvider = FakeStringProvider()
      val viewModel = PeopleViewModel(repository, stringProvider, EffectDelegate())

      advanceUntilIdle()
      assertEquals(stringProvider.getString(R.string.error_people_observe), viewModel.stateFlow.value.loadError)
      assertFalse(viewModel.stateFlow.value.isLoading)

      repository.peopleFlow.value = Result.success(emptyList())
      advanceUntilIdle()
      assertEquals(null, viewModel.stateFlow.value.loadError)
   }
}
