package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.cars.list.CarsEvent
import de.rogallab.mobile.ui.cars.list.CarsIntent
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.coordinator.CoordinatorEffect
import kotlinx.coroutines.flow.Flow

// Stateful adapter between the list ViewModel and its stateless screen.
//
// It collects state and one-shot events, coordinates the scroll after Undo
// and forwards only UI callbacks. Koin and ViewModel creation stay in the
// central Navigation 3 entry provider.

private const val TAG = "<-CarsAdapter"

@Composable
fun CarsAdapter(
   viewModel: CarsViewModel,
   coordinatorEvents: Flow<CoordinatorEffect.RestoreCar>,
   contentPadding: PaddingValues,
   onCreate: () -> Unit,
   onOpen: (String) -> Unit,
   onRemove: (Car, Int) -> Unit,
   onMessage: (UiText) -> Unit,
) {
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount.intValue++}") }

   val carListState by viewModel.state.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { carListEvent ->
         when (carListEvent) {
            CarsEvent.NavigateToCreate -> onCreate()
            is CarsEvent.NavigateToDetails -> onOpen(carListEvent.carId)
            is CarsEvent.RequestRemove -> onRemove(
               carListEvent.car,
               carListEvent.originalIndex,
            )
            is CarsEvent.ShowSnackbar -> onMessage(carListEvent.message)
         }
      }
   }

   LaunchedEffect(viewModel, coordinatorEvents) {
      coordinatorEvents.collect { coordinatorEvent ->
         viewModel.onIntent(
            CarsIntent.Restore(
               car = coordinatorEvent.car,
               originalIndex = coordinatorEvent.originalIndex,
            )
         )
      }
   }

   LaunchedEffect(carListState.restoredCarId, carListState.cars) {
      val restoredCarId = carListState.restoredCarId ?: return@LaunchedEffect
      val restoredIndex = carListState.cars.indexOfFirst { car ->
         car.id == restoredCarId
      }
      if (restoredIndex >= 0) {
         lazyListState.animateScrollToItem(restoredIndex)
      }
      viewModel.onIntent(CarsIntent.Restored)
   }

   CarsScreen(
      carListState = carListState,
      lazyListState = lazyListState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
