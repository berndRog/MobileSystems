package de.rogallab.mobile.ui.coordinator

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.ui.common.resolve

// Resolves resource-backed messages and runs the app-wide Snackbar/Undo flow.
@Composable
fun CoordinatorEffectHandler(
   coordinatorViewModel: CoordinatorViewModel,
   snackbarHostState: SnackbarHostState,
) {
   val context = LocalContext.current
   val coordinatorState by coordinatorViewModel.state.collectAsStateWithLifecycle()

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

      if (snackbarResult == SnackbarResult.ActionPerformed) {
         coordinatorViewModel.onIntent(
            CoordinatorIntent.UndoRemove(uiMessage.id)
         )
      }
      else if (uiMessage.actionLabel != null) {
         coordinatorViewModel.onIntent(
            CoordinatorIntent.ConfirmRemove(uiMessage.id)
         )
      }

      coordinatorViewModel.onIntent(
         CoordinatorIntent.MessageConsumed(uiMessage.id)
      )
   }
}
