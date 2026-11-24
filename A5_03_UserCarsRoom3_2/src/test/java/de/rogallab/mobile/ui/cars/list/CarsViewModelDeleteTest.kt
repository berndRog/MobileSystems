package de.rogallab.mobile.ui.cars.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarRepository
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
class CarsViewModelDeleteTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()
   private val golf = Car(manufacturer = "VW", model = "Golf", id = "c1")
   private val strings = FakeStringProvider()

   private fun create(repository: FakeCarRepository) = CarsViewModel(
      _repository = repository,
      _personRepository = FakePersonRepository(),
      _stringProvider = strings,
      _effectDelegate = EffectDelegate(),
   )

   @Test
   fun requestRemove_emitsConfirmationWithoutDeleting() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeCarRepository(listOf(golf))
      val viewModel = create(repository)
      advanceUntilIdle()
      viewModel.effects.test {
         viewModel.onIntent(CarsIntent.RequestRemove(golf.id))
         advanceUntilIdle()
         val effect = awaitItem() as CarsEffect.ConfirmRemove
         assertEquals(golf.id, effect.carId)
         assertEquals(strings.getString(R.string.action_confirm), effect.actionLabel)
         assertEquals(emptyList<Car>(), repository.removed)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun confirmRemove_deletesCar() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeCarRepository(listOf(golf))
      val viewModel = create(repository)
      advanceUntilIdle()
      viewModel.onIntent(CarsIntent.ConfirmRemove(golf.id))
      advanceUntilIdle()
      assertEquals(listOf(golf), repository.removed)
      assertEquals(emptyList<Car>(), viewModel.stateFlow.value.cars)
   }
}
