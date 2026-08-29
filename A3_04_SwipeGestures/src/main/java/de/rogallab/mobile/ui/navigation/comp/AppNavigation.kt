package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.navigation.NavigationAnimations
import de.rogallab.mobile.ui.navigation.PeopleKey
import de.rogallab.mobile.ui.navigation.PersonKey
import de.rogallab.mobile.ui.navigation.PopReason
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TAG = "<-AppNavigation"

@Composable
fun AppNavigation() {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(TAG, "Composition #${nComp.intValue++}") }

   // Reuse the saveable Navigation 3 back stack introduced in A3_03.
   val backStack = rememberNavBackStack(PeopleKey)

   // One SnackbarHostState is shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   // The controller and its CoroutineScope live above NavDisplay. Therefore a
   // Snackbar started by one destination can remain active after navigation.
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   // Selects the visible pop animation for Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.CANCEL) }

   // Logs the initial or restored back stack.
   LaunchedEffect(backStack) {
      logNavigationOperation(
         operation = "initial",
         destination = backStack.lastOrNull(),
         backStack = backStack,
      )
   }

   Scaffold(
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState)
      },
      contentWindowInsets = WindowInsets.safeDrawing.add(
         WindowInsets(top = 0.dp, bottom = 0.dp)
      ),
      modifier = Modifier.fillMaxSize(),
   ) { contentPadding ->

      NavDisplay(
         backStack = backStack,

         // Handles system back navigation as a cancel operation.
         onBack = {
            currentPopReason = PopReason.CANCEL
            pop(backStack)
         },

         // Preserves saveable Compose state and ViewModels per NavEntry.
         entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(
               rememberSaveableStateHolder()
            ),
            rememberViewModelStoreNavEntryDecorator(),
         ),

         // Navigation animations remain unchanged from the preceding step.
         transitionSpec = NavigationAnimations.enterTransitionSpec,
         popTransitionSpec =
            NavigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec =
            NavigationAnimations.predictivePopTransitionSpec,

         entryProvider = entryProvider {

            // Root destination: list of people.
            entry<PeopleKey> {
               val peopleViewModel = koinViewModel<PeopleViewModel>()

               PeopleAdapter(
                  viewModel = peopleViewModel,
                  modifier = Modifier
                     .padding(contentPadding)
                     .fillMaxSize(),

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  // Confirm the destructive action before the repository is changed.
                  onConfirmRemove = { message, actionLabel, personId ->
                     snackbarController.showAction(
                        message = message,
                        actionLabel = actionLabel,
                        onAction = {
                           peopleViewModel.onIntent(
                              PeopleIntent.ConfirmRemove(personId)
                           )
                        },
                     )
                  },

                  onNavigateBack = {
                     currentPopReason = PopReason.CANCEL
                     pop(backStack)
                  },

                  // null -> create, id -> detail.
                  onNavigateTo = { personId ->
                     push(
                        destination = PersonKey(personId),
                        backStack = backStack,
                     )
                  },
               )
            }

            // Person editing is unchanged from A3_03. A3_04 adds Swipe only to
            // the list feature; image selection is intentionally not introduced yet.
            entry<PersonKey> { personKey ->
               val personViewModel = koinViewModel<PersonViewModel> {
                  parametersOf(personKey.personId)
               }

               PersonAdapter(
                  viewModel = personViewModel,
                  modifier = Modifier
                     .padding(contentPadding)
                     .fillMaxSize(),

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  onNavigateBack = { reason ->
                     currentPopReason =
                        when (reason) {
                           BackReason.Save -> PopReason.SAVE
                           BackReason.Cancel -> PopReason.CANCEL
                        }
                     pop(backStack)
                  },
               )
            }
         },
      )
   }
}

// Adds a destination to the standard Navigation 3 back stack.
private fun push(
   destination: NavKey,
   backStack: MutableList<NavKey>,
) {
   backStack.add(destination)

   logNavigationOperation(
      operation = "push",
      destination = destination,
      backStack = backStack,
   )
}

// Removes the current destination while preserving the root destination.
private fun pop(
   backStack: MutableList<NavKey>,
) {
   if (backStack.size > 1) {
      val removedDestination = backStack.removeLastOrNull()
      logNavigationOperation(
         operation = "pop",
         destination = removedDestination,
         backStack = backStack,
      )
   }
   else {
      logNavigationOperation(
         operation = "pop ignored - root remains",
         destination = backStack.lastOrNull(),
         backStack = backStack,
      )
   }
}

// Logs one navigation operation and the complete current back stack.
private fun logNavigationOperation(
   operation: String,
   destination: NavKey?,
   backStack: List<NavKey>,
) {
   val destinationText = destination?.toString() ?: "none"
   val stackText = backStack.joinToString(
      separator = " -> ",
      prefix = "[",
      postfix = "]",
   )

   Alog.d(
      TAG,
      "$operation: $destinationText | stack(${backStack.size}) = $stackText",
   )
}

/*
 * Didaktik und Lernziele
 *
 * - A3_04_SwipeGestures baut unmittelbar auf A3_03_Navigation auf. Der
 *   Navigation-3-Back-Stack, die Effects und die Navigationsanimationen bleiben
 *   erhalten. Neu hinzu kommen die Swipe-Gesten und die Bestätigung vor Delete.
 *
 * - Navigation und Meldungsausgabe bleiben bewusst getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *      ConfirmRemove             -> Action-Snackbar
 *
 * - Swipe-to-Detail verwendet denselben NavigateTo-Fluss wie ein Tap auf eine
 *   PersonCard. Swipe-to-Delete fordert dagegen zunächst nur die Bestätigung an.
 *
 * - Die Action-Snackbar führt nur bei ActionPerformed zu
 *   PeopleIntent.ConfirmRemove. Wird sie verworfen oder läuft sie ab, bleibt das
 *   Repository unverändert.
 *
 * - PeopleViewModel kann weiterhin innerhalb des PeopleKey-Eintrags erzeugt
 *   werden. A3_04 hält keinen pending Delete-State. Erst A3_05 benötigt einen
 *   länger lebenden ViewModel-State für visuelles Entfernen und Undo.
 *
 * Lernziele:
 *
 * - Den in A3_03 aufgebauten State-/Effect-/Navigation-Fluss weiterverwenden.
 * - Swipe-Gesten als Compose-nahe Erweiterung ergänzen.
 * - Eine destruktive Aktion mit einer Action-Snackbar bestätigen.
 * - Delete-Bestätigung von Undo klar unterscheiden.
 */
