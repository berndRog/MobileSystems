package de.rogallab.mobile.ui.cars.input_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarRepository
import de.rogallab.mobile.testing.FakeImageEdit
import de.rogallab.mobile.testing.FakeImageFileStorage
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CarLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun missingCar_keepsErrorUntilRetryFindsIt() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeCarRepository()
      val car = Car(manufacturer = "VW", model = "Golf", id = "c1")
      val viewModel = CarViewModel(
         carId = car.id,
         _carRepository = repository,
         _personRepository = FakePersonRepository(),
         _stringProvider = FakeStringProvider(),
         _validator = CarValidator(ApplicationProvider.getApplicationContext()),
         _imageFileStorage = FakeImageFileStorage(),
         _imageEdit = FakeImageEdit(),
         _effectDelegate = EffectDelegate(),
      )
      advanceUntilIdle()

      assertTrue(viewModel.stateFlow.value.notFound)
      assertTrue(viewModel.stateFlow.value.loadError != null)
      assertFalse(viewModel.stateFlow.value.isLoading)

      repository.carsFlow.value = Result.success(listOf(car))
      viewModel.onIntent(CarIntent.RetryLoad)
      advanceUntilIdle()

      assertEquals(car, viewModel.stateFlow.value.car)
      assertEquals(null, viewModel.stateFlow.value.loadError)
      assertFalse(viewModel.stateFlow.value.notFound)
   }
}
