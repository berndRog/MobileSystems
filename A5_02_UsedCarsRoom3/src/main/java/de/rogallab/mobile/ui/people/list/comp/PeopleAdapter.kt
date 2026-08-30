package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.coordinator.CoordinatorEffect
import de.rogallab.mobile.ui.people.list.PeopleEfect
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import kotlinx.coroutines.flow.Flow

// Stateful adapter between the list ViewModel and its stateless screen.
//
// It collects state and one-shot events, coordinates the scroll after Undo
// and forwards only UI callbacks. Koin and ViewModel creation stay in the
// central Navigation 3 entry provider.

private const val TAG = "<-PeopleAdapter"

@Composable
fun PeopleAdapter(
   viewModel: PeopleViewModel,
   coordinatorEvents: Flow<CoordinatorEffect.RestorePerson>,
   contentPadding: PaddingValues,
   onCreate: () -> Unit,
   onOpen: (String) -> Unit,
   onRemove: (Person, Int) -> Unit,
   onMessage: (UiText) -> Unit,
) {
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount.intValue++}") }

   val peopleUiState by viewModel.state.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { peopleEvent ->
         when (peopleEvent) {
            PeopleEfect.NavigateToCreate -> onCreate()
            is PeopleEfect.NavigateToDetails ->
               onOpen(peopleEvent.personId)
            is PeopleEfect.RequestRemove -> onRemove(
               peopleEvent.person,
               peopleEvent.originalIndex,
            )
            is PeopleEfect.ShowSnackbar ->
               onMessage(peopleEvent.message)
         }
      }
   }

   LaunchedEffect(viewModel, coordinatorEvents) {
      coordinatorEvents.collect { coordinatorEvent ->
         viewModel.onIntent(
            PeopleIntent.Restore(
               person = coordinatorEvent.person,
               originalIndex = coordinatorEvent.originalIndex,
            )
         )
      }
   }

   LaunchedEffect(
      peopleUiState.restoredPersonId,
      peopleUiState.people,
   ) {
      val restoredPersonId =
         peopleUiState.restoredPersonId ?: return@LaunchedEffect
      val restoredIndex = peopleUiState.people.indexOfFirst { person ->
         person.id == restoredPersonId
      }
      if (restoredIndex >= 0) {
         lazyListState.animateScrollToItem(restoredIndex)
      }
      viewModel.onIntent(PeopleIntent.Restored)
   }

   PeopleScreen(
      peopleUiState = peopleUiState,
      lazyListState = lazyListState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
