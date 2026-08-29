package de.rogallab.mobile.ui.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.ui.common.resolve
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorEvent
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorIntent
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorViewModel
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel


@Composable
fun PeopleCoordinatorEffectHandler(
   coordinatorViewModel: PeopleCoordinatorViewModel,
   personListViewModel: PeopleViewModel,
   snackbarHostState: SnackbarHostState,
) {
   val context = LocalContext.current
   val coordinatorState by coordinatorViewModel.state.collectAsStateWithLifecycle()

   // Collects one-time events sent by the coordinator.
   // --------------------------------------------------------------------------
   // A RestorePerson event is produced when the user selects the Undo action
   // of the Snackbar.
   LaunchedEffect(coordinatorViewModel, personListViewModel) {
      coordinatorViewModel.events.collect { coordinatorEvent ->
         when (coordinatorEvent) {
            is PeopleCoordinatorEvent.RestorePerson -> {
               personListViewModel.onIntent(
                  PeopleIntent.Restore(person = coordinatorEvent.person,
                     originalIndex = coordinatorEvent.originalIndex ))
            }
         }
      }
   }

   // Snackbar-Workflow: Displays every new coordinator message exactly once.
   // --------------------------------------------------------------------------
   // Message ID is used as the effect key. A new message therefore starts
   // a new Snackbar workflow, while recompositions with the same message do
   // not display the Snackbar repeatedly.
   LaunchedEffect(coordinatorState.message?.id) {
      val uiMessage = coordinatorState.message ?: return@LaunchedEffect

      val snackbarResult = snackbarHostState.showSnackbar(
         message = uiMessage.text.resolve(context),
         actionLabel = uiMessage.actionLabel?.resolve(context),
         withDismissAction = uiMessage.actionLabel == null,
         duration =
            if (uiMessage.actionLabel == null) SnackbarDuration.Short
            else SnackbarDuration.Long,
      )

      // Selecting Undo requests restoration of the temporarily removed person.
      if (snackbarResult == SnackbarResult.ActionPerformed) {
         coordinatorViewModel.onIntent(
            PeopleCoordinatorIntent.UndoRemove(uiMessage.id))
      }
      // When a delete Snackbar disappears without Undo, the removal is
      // confirmed and can be persisted permanently.
      else if (uiMessage.actionLabel != null) {
         coordinatorViewModel.onIntent(
            PeopleCoordinatorIntent.ConfirmRemove(uiMessage.id))
      }

      // Removes the processed message from the coordinator state so it is not
      // displayed again during a later recomposition
      coordinatorViewModel.onIntent(
         PeopleCoordinatorIntent.MessageConsumed(id = uiMessage.id))
   }
}