package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.navigation.NavigationAnimations
import de.rogallab.mobile.ui.navigation.PeopleKey
import de.rogallab.mobile.ui.navigation.PopReason
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.compose.viewmodel.koinViewModel

private const val TAG = "<-AppNavigation"

@Composable
fun AppNavigation() {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(TAG, "Composition #${nComp.intValue++}") }

   // Creates the standard saveable Navigation 3 back stack.
   val backStack = rememberNavBackStack(PeopleKey)

   // Pending Undo callbacks must survive navigation to the person destination.
   val peopleViewModel = koinViewModel<PeopleViewModel>()

   // One SnackbarHostState and one visible host are shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   // The controller and its CoroutineScope live above NavDisplay. Therefore a
   // Snackbar started by one destination can remain active after navigation.
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   // Selects the visible remove animation for Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.Cancel) }

   val navEntryProvider = appNavEntryProvider(
      backStack = backStack,
      onMessage = snackbarController::showMessage,
      onError = snackbarController::showError,
      peopleViewModel = peopleViewModel,
      onUndo = { message, actionLabel, onAction, onDismiss ->
         snackbarController.showAction(message, actionLabel, onAction, onDismiss)
      },
      onNavigateBack = { reason ->
         currentPopReason = reason
         remove(backStack)
      },
      onNavigateTo = { destination -> add(destination, backStack) },
   )

   // Logs the initial or restored back stack.
   LaunchedEffect(backStack) {
      logNavigationOperation("initial", backStack.lastOrNull(), backStack)
   }

   Scaffold(
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding()
         )
      },
   ) { innerPadding ->
      NavDisplay(
         modifier = Modifier
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding),

         backStack = backStack,

         // Handles system back navigation as a cancel operation.
         onBack = {
            currentPopReason = PopReason.Cancel
            remove(backStack)
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

         entryProvider = navEntryProvider,
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Navigation ist wie in A3_03 auf drei Dateien verteilt:
 *   AppNavigation hält Backstack und UI-Rahmen, AppNavEntryProvider ordnet
 *   NavKeys den Screens zu, BackStackOperations enthält add/remove und Logging.
 *   Zusätzliche Screen-Aktionen werden als Callback an den Provider übergeben.
 *
 * - A3_05_SwipeDeleteUndo verwendet den standardmäßigen saveable Navigation-3-Back-Stack:
 *
 *      val backStack = rememberNavBackStack(PeopleKey)
 *
 * - Navigation und Meldungsausgabe bleiben bewusst getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *
 * - SnackbarController und der einzige sichtbare SnackbarHost liegen oberhalb
 *   des NavDisplay. Der CoroutineScope bleibt bei einem Destination-Wechsel bestehen.
 *   Eine Meldung kann so nach erfolgreichem Speichern und NavigateBack auf der
 *   People-Liste weiter angezeigt werden.
 *
 * - Ladefehler bleiben im jeweiligen Screen-State sichtbar; Retry startet
 *   einen neuen Ladeversuch. Fehler einzelner Aktionen nutzen ShowError.
 *
 * - Eine eigene Coordinator-Queue ist nicht erforderlich. SnackbarHostState
 *   serialisiert gleichzeitig angeforderte Snackbars bereits selbst.
 *
 * - Save und Cancel setzen unterschiedliche PopReason-Werte. Die vorhandenen
 *   Animationen machen damit die Art der Rücknavigation sichtbar.
 *   System-Back setzt Cancel und entfernt den aktuellen Backstack-Eintrag.
 *   Verspätete Navigationseffekte ändern nur ihren aktuellen Backstack-Eintrag.
 *
 * - ShowUndo wird als Action-Snackbar gezeigt. onAction stellt die Person
 *   wieder her, onDismiss startet das endgültige Löschen im Repository.

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
