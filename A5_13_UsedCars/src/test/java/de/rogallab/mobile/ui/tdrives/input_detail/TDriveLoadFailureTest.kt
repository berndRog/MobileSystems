package de.rogallab.mobile.ui.tdrives.input_detail

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.testing.FakeCarRepository
import de.rogallab.mobile.testing.FakePersonRepository
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.FakeTDriveRepository
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
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TDriveLoadFailureTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   @Test
   fun missingDrive_keepsErrorUntilRetryFindsIt() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeTDriveRepository()
      val drive = TDrive(id = "t1", start = Instant.parse("2026-08-30T12:00:00Z"))
      val viewModel = TDriveViewModel(
         tDriveId = drive.id,
         _tDriveRepository = repository,
         _personRepository = FakePersonRepository(),
         _carRepository = FakeCarRepository(),
         _stringProvider = FakeStringProvider(),
         _validator = TDriveValidator(ApplicationProvider.getApplicationContext()),
         _effectDelegate = EffectDelegate(),
      )
      advanceUntilIdle()

      assertTrue(viewModel.stateFlow.value.notFound)
      assertTrue(viewModel.stateFlow.value.loadError != null)
      assertFalse(viewModel.stateFlow.value.isLoading)

      repository.tDrivesFlow.value = Result.success(listOf(drive))
      viewModel.onIntent(TDriveIntent.RetryLoad)
      advanceUntilIdle()

      assertEquals(drive, viewModel.stateFlow.value.tDrive)
      assertEquals(null, viewModel.stateFlow.value.loadError)
      assertFalse(viewModel.stateFlow.value.notFound)
   }
}
