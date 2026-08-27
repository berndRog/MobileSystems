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

   // Creates the standard saveable Navigation 3 back stack.
   val backStack = rememberNavBackStack(PeopleKey)

   // The list ViewModel is kept above NavDisplay so pending visual removals
   // and Undo callbacks survive navigation to a person destination.
   val peopleViewModel = koinViewModel<PeopleViewModel>()

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

         // Keeps the visible navigation animations from the previous project.
         transitionSpec = NavigationAnimations.enterTransitionSpec,
         popTransitionSpec =
            NavigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec =
            NavigationAnimations.predictivePopTransitionSpec,

         entryProvider = entryProvider {

            // Root destination: list of people.
            entry<PeopleKey> {

               PeopleAdapter(
                  viewModel = peopleViewModel,
                  modifier = Modifier
                     .padding(contentPadding)
                     .fillMaxSize(),

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  // The person is only hidden from the UI at this point.
                  // The Snackbar result decides between Undo and repository commit.
                  onUndo = { message, actionLabel, personId ->
                     snackbarController.showAction(
                        message = message,
                        actionLabel = actionLabel,
                        onAction = {
                           peopleViewModel.onIntent(
                              PeopleIntent.UndoRemove(personId)
                           )
                        },
                        onDismiss = {
                           peopleViewModel.onIntent(
                              PeopleIntent.CommitRemove(personId)
                           )
                        },
                     )
                  },

                  onBack = {
                     currentPopReason = PopReason.CANCEL
                     pop(backStack)
                  },

                  // null -> create, id -> detail/edit.
                  onNavigateTo = { personId ->
                     push(
                        destination = PersonKey(personId),
                        backStack = backStack,
                     )
                  },
               )
            }

            // Shared destination for create and edit.
            entry<PersonKey> { personKey ->
               val personViewModel = koinViewModel<PersonViewModel> {
                  parametersOf(personKey.personId)
               }

               PersonAdapter(
                  viewModel = personViewModel,
                  modifier = Modifier
                     .padding(contentPadding)
                     .fillMaxSize(),

                  // showMessage() starts its coroutine in this navigation-level
                  // controller before NavigateBack removes the Person destination.
                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  onBack = { reason ->
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
 * - A3_05_SwipeGestures verwendet den standardmäßigen saveable Navigation-3-Back-Stack:
 *
 *      val backStack = rememberNavBackStack(PeopleKey)
 *
 * - Navigation und Meldungsausgabe bleiben bewusst getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *
 * - Der SnackbarController wird oberhalb des NavDisplay erzeugt. Sein
 *   CoroutineScope bleibt deshalb bei einem Destination-Wechsel bestehen.
 *   Eine Meldung kann so nach erfolgreichem Speichern und NavigateBack auf der
 *   People-Liste weiter angezeigt werden.
 *
 * - Ein Ladefehler im PersonScreen erzeugt dagegen nur ShowError. Es findet
 *   keine Navigation statt und die Fehlermeldung erscheint im selben Screen.
 *
 * - Eine eigene Coordinator-Queue ist nicht erforderlich. SnackbarHostState
 *   serialisiert gleichzeitig angeforderte Snackbars bereits selbst.
 *
 * - Save und Cancel setzen unterschiedliche PopReason-Werte. Die vorhandenen
 *   Animationen machen damit die Art der Rücknavigation sichtbar.
 *
 * - PeopleViewModel wird oberhalb des NavDisplay erzeugt. Dadurch bleiben
 *   pending Removals und die Undo-Zuordnung erhalten, auch wenn zwischenzeitlich
 *   zum PersonScreen navigiert wird.
 *
 * - Die beiden Ergebnisse der Action-Snackbar werden getrennt behandelt:
 *
 *      ActionPerformed -> PeopleIntent.UndoRemove
 *      Dismissed       -> PeopleIntent.CommitRemove
 *
 *   Das Repository wird damit erst angefasst, wenn die Undo-Möglichkeit nicht
 *   genutzt wurde.
 *
 * Lernziele:
 *
 * - Navigation 3 mit rememberNavBackStack, NavDisplay und NavKey einsetzen.
 * - Screenübergreifende Meldungen mit einem navigationweit lebenden
 *   SnackbarController darstellen.
 * - Navigation und globale UI-Meldungen als getrennte Verantwortlichkeiten
 *   verstehen.
 * - Vorhandene Material-3-Infrastruktur statt einer eigenen Message-Queue nutzen.
 * - Unterschiedliche Navigationsarten über Animationen sichtbar machen.
 */
