package de.rogallab.mobile.ui.cars.list

import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.data.network.ServerUnreachableException
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
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], application = android.app.Application::class)
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

   @Test
   fun ordinaryFailureAndCancellation_keepFallbackAndAvoidDuplicateLogs() =
      runTest(mainDispatcherRule.testDispatcher) {
         de.rogallab.mobile.shared.domain.utilities.Alog.set(useAndroidLog = true)
         for (cancelled in listOf(false, true)) {
            org.robolectric.shadows.ShadowLog.clear()
            val repository = FakeCarRepository(listOf(golf)).apply {
               removeResult = Result.failure(if (cancelled)
                  kotlinx.coroutines.CancellationException("cancelled") else IllegalStateException("failed"))
            }
            val vm = create(repository)
            val store = androidx.lifecycle.ViewModelStore().also { it.put("cars", vm) }
            advanceUntilIdle()
            vm.effects.test {
               vm.onIntent(CarsIntent.ConfirmRemove(golf.id))
               advanceUntilIdle()
               if (!cancelled) assertEquals(
                  CarsEffect.ShowError(strings.getString(R.string.error_car_delete)), awaitItem())
               assertEquals(if (cancelled) 0 else 1,
                  org.robolectric.shadows.ShadowLog.getLogsForTag("<-CarsViewModel").count { it.type == android.util.Log.ERROR })
               assertEquals(listOf(golf), vm.stateFlow.value.cars)
               expectNoEvents()
            }
            store.clear()
         }
      }

   @Test
   fun networkFailure_emitsSharedNetworkMessage() =
      runTest(mainDispatcherRule.testDispatcher) {
         val networkMessage = "Der Server ist nicht erreichbar."
         val repository = FakeCarRepository(listOf(golf)).apply {
            removeResult = Result.failure(
               ServerUnreachableException(networkMessage)
            )
         }
         val viewModel = create(repository)
         advanceUntilIdle()

         viewModel.effects.test {
            viewModel.onIntent(CarsIntent.ConfirmRemove(golf.id))
            advanceUntilIdle()

            val error = awaitItem() as CarsEffect.ShowError
            assertEquals(networkMessage, error.message)

            cancelAndIgnoreRemainingEvents()
         }
      }
}
