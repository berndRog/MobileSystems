package de.rogallab.mobile.ui.tdrives.list

import app.cash.turbine.test
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
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TDrivesViewModelDeleteTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val drive = TDrive(
      id = "t1",
      personId = "p1",
      carId = "c1",
      start = LocalDateTime(2026, 8, 30, 14, 0),
   )

   private fun create(repository: FakeTDriveRepository) = TDrivesViewModel(
      _tDriveRepository = repository,
      _personRepository = FakePersonRepository(),
      _carRepository = FakeCarRepository(),
      _stringProvider = FakeStringProvider(),
      _effectDelegate = EffectDelegate(),
   )

   @Test
   fun requestRemove_emitsConfirmationWithoutDeleting() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeTDriveRepository(listOf(drive))
      val viewModel = create(repository)
      advanceUntilIdle()

      viewModel.effects.test {
         viewModel.onIntent(TDrivesIntent.RequestRemove(drive.id))
         advanceUntilIdle()

         val effect = awaitItem() as TDrivesEffect.ConfirmRemove
         assertEquals(drive.id, effect.tDriveId)
         assertEquals(emptyList<TDrive>(), repository.removed)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun confirmRemove_deletesTestDrive() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeTDriveRepository(listOf(drive))
      val viewModel = create(repository)
      advanceUntilIdle()

      viewModel.onIntent(TDrivesIntent.ConfirmRemove(drive.id))
      advanceUntilIdle()

      assertEquals(listOf(drive), repository.removed)
      assertEquals(emptyList<TDrive>(), viewModel.stateFlow.value.tDrives)
   }
}
