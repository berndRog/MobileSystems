package de.rogallab.mobile.ui.tdrives.list.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.coordinator.CoordinatorEffect
import de.rogallab.mobile.ui.tdrives.list.TDrivesEvent
import de.rogallab.mobile.ui.tdrives.list.TDrivesIntent
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import kotlinx.coroutines.flow.Flow

// Stateful adapter between the list ViewModel and its stateless screen.
//
// It collects state and one-shot events, coordinates the scroll after Undo
// and forwards only UI callbacks. Koin and ViewModel creation stay in the
// central Navigation 3 entry provider.
@Composable
fun TDrivesAdapter(
   viewModel: TDrivesViewModel,
   coordinatorEvents: Flow<CoordinatorEffect.RestoreTestDrive>,
   contentPadding: PaddingValues,
   onCreate: () -> Unit,
   onOpen: (String) -> Unit,
   onRemove: (TDrive, Int) -> Unit,
   onMessage: (UiText) -> Unit,
) {
   val tag = "<-TdrivesAdapter"
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(tag, "Composition #${cCount.intValue++}") }

   val testDriveListState by viewModel.state.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { testDriveListEvent ->
         when (testDriveListEvent) {
            TDrivesEvent.NavigateToCreate -> onCreate()
            is TDrivesEvent.NavigateToDetails ->
               onOpen(testDriveListEvent.testDriveId)
            is TDrivesEvent.RequestRemove -> onRemove(
               testDriveListEvent.tDrive,
               testDriveListEvent.originalIndex,
            )
            is TDrivesEvent.ShowSnackbar ->
               onMessage(testDriveListEvent.message)
         }
      }
   }

   LaunchedEffect(viewModel, coordinatorEvents) {
      coordinatorEvents.collect { coordinatorEvent ->
         viewModel.onIntent(
            TDrivesIntent.Restore(
               tDrive = coordinatorEvent.tDrive,
               originalIndex = coordinatorEvent.originalIndex,
            )
         )
      }
   }

   LaunchedEffect(
      testDriveListState.restoredTDriveId,
      testDriveListState.tDrives,
   ) {
      val restoredTestDriveId =
         testDriveListState.restoredTDriveId ?: return@LaunchedEffect
      val restoredIndex = testDriveListState.tDrives.indexOfFirst { testDrive ->
         testDrive.id == restoredTestDriveId
      }
      if (restoredIndex >= 0) {
         lazyListState.animateScrollToItem(restoredIndex)
      }
      viewModel.onIntent(TDrivesIntent.Restored)
   }

   TDrivesScreen(
      tDrivesUiState = testDriveListState,
      lazyListState = lazyListState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
