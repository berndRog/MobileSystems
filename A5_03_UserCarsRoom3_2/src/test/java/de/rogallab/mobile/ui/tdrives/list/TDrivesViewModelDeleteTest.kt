package de.rogallab.mobile.ui.tdrives.list

import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import de.rogallab.mobile.domain.usecases.TDriveUcDelete
import de.rogallab.mobile.testing.FakeTDriveReadModel
import de.rogallab.mobile.testing.FakeStringProvider
import de.rogallab.mobile.testing.FakeTDriveRepository
import de.rogallab.mobile.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TDrivesViewModelDeleteTest {
   @get:Rule val mainDispatcherRule = MainDispatcherRule()

   private val drive = TDrive(
      id = "t1",
      personId = "p1",
      carId = "c1",
      start = Instant.parse("2026-08-30T12:00:00Z"),
      isCompleted = false,
   )

   private val driveItem = TDriveListItem("t1", "Ada Lovelace", "VW Golf", drive.start, false)

   private fun create(repository: FakeTDriveRepository, readModel: FakeTDriveReadModel) = TDrivesViewModel(
      _readModel = readModel,
      _tDriveUcDelete = TDriveUcDelete(repository),
      _stringProvider = FakeStringProvider(),
      _effectDelegate = EffectDelegate(),
   )

   @Test
   fun requestRemove_emitsConfirmationWithoutDeleting() = runTest(mainDispatcherRule.testDispatcher) {
      val repository = FakeTDriveRepository(listOf(drive))
      val readModel = FakeTDriveReadModel(listOf(driveItem))
      val viewModel = create(repository, readModel)
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
      val readModel = FakeTDriveReadModel(listOf(driveItem))
      val viewModel = create(repository, readModel)
      advanceUntilIdle()

      viewModel.onIntent(TDrivesIntent.ConfirmRemove(drive.id))
      advanceUntilIdle()

      assertEquals(listOf(drive), repository.removed)
      // The list changes only when the read model emits the new projection.
      assertEquals(listOf(driveItem), viewModel.stateFlow.value.tDrives)
      readModel.itemsFlow.value = Result.success(emptyList())
      advanceUntilIdle()
      assertEquals(emptyList<TDriveListItem>(), viewModel.stateFlow.value.tDrives)
   }
}
