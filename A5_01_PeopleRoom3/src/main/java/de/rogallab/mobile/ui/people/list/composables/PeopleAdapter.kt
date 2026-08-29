package de.rogallab.mobile.ui.people.list.composables

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
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.people.list.PeopleEvent
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleScreen
import de.rogallab.mobile.ui.people.list.PeopleViewModel

/**
 * Stateful adapter between PersonListViewModel and PersonListScreen.
 *
 * Koin and ViewModel creation stay in PeopleNavigation. This function only
 * collects flows, performs the one-shot scroll after Undo and connects the
 * ViewModel to the stateless screen.
 */
@Composable
fun PeopleAdapter(
   viewModel: PeopleViewModel,
   contentPadding: PaddingValues,
   onCreate: () -> Unit,
   onOpen: (String) -> Unit,
   onRemove: (Person, Int) -> Unit,
   onMessage: (UiText) -> Unit,
) {
   val tag = "<-PeopleListAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }


   val peopleUiState by viewModel.state.collectAsStateWithLifecycle()
   val listState = rememberLazyListState()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { event ->
         when (event) {
            PeopleEvent.NavigateToCreate -> onCreate()
            is PeopleEvent.NavigateToDetails -> onOpen(event.personId)
            is PeopleEvent.RequestRemove -> onRemove(
               event.person,
               event.originalIndex
            )

            is PeopleEvent.ShowSnackbar -> onMessage(event.message)
         }
      }
   }

   LaunchedEffect(peopleUiState.restoredPersonId, peopleUiState.people) {
      val restoredId = peopleUiState.restoredPersonId ?: return@LaunchedEffect
      val index = peopleUiState.people.indexOfFirst { person -> person.id == restoredId }

      if (index >= 0) {
         listState.animateScrollToItem(index)
      }
      viewModel.onIntent(PeopleIntent.Restored)
   }

   PeopleScreen(
      peopleUiState = peopleUiState,
      listState = listState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent
   )
}
