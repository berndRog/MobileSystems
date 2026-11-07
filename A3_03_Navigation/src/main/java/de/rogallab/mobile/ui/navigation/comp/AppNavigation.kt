package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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

private const val TAG = "<-AppNavigation"

@Composable
fun AppNavigation() {

   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(TAG, "Composition #${nComp.intValue++}") }

   // Selects the visible remove animation for Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.Cancel) }

   // One SnackbarHostState and one visible host are shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   // The controller and its CoroutineScope live above NavDisplay. Therefore a
   // Snackbar started by one destination can remain active after navigation.
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   // Creates the standard saveable Navigation 3 back stack.
   val backStack = rememberNavBackStack(PeopleKey)

   val navEntryProvider = appNavEntryProvider(
      backStack = backStack,
      onMessage = snackbarController::showMessage,
      onError = snackbarController::showError,
      onNavigateBack = { reason ->
         currentPopReason = reason
         remove(backStack)
      },
      onNavigateTo = { destination ->
         add(destination, backStack)
      },
   )

   val snackbarBottomPadding = calculateSnackbarBottomPadding(
      showListFab = backStack.lastOrNull() is PeopleKey,
   )

   // Logs the initial or restored back stack.
   LaunchedEffect(backStack) {
      logNavigationOperation("initial", backStack.lastOrNull(), backStack)
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(bottom = snackbarBottomPadding),
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

@Composable
private fun calculateSnackbarBottomPadding(showListFab: Boolean): Dp {

   val listFabSnackbarClearance = 88.dp // 56 dp FAB + 16 dp edge + 16 dp gap

   // Scaffold already places its snackbar above the system bar. Only add the
   // larger of the remaining IME height and the clearance needed for the FAB.
   val density = LocalDensity.current
   val systemBarBottom = ScaffoldDefaults.contentWindowInsets.getBottom(density)
   val imeBottom = WindowInsets.ime.getBottom(density)
   val fabClearance = with(density) {
      if (showListFab) listFabSnackbarClearance.roundToPx() else 0
   }
   return with(density) {
      maxOf((imeBottom - systemBarBottom).coerceAtLeast(0), fabClearance).toDp()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_03_Navigation verwendet den standardmäßigen saveable Navigation-3-Back-Stack:
 *
 *      val backStack = rememberNavBackStack(PeopleKey)
 *
 * - Die Verantwortlichkeiten sind auf drei Dateien im selben Package verteilt:
 *
 *      AppNavigation.kt       -> Scaffold, SnackbarHost, NavDisplay, Animationen
 *      AppNavEntryProvider.kt -> Zuordnung NavKey -> Screen (Adapter + ViewModel)
 *      BackStackOperations.kt -> navigateTo / navigateBack / Logging
 *
 *   Dadurch bleibt AppNavigation übersichtlich. Der Provider enthält keine
 *   Navigationslogik, sondern nur Zuordnung und Guards. Die Stack-Operationen
 *   brauchen weder Compose noch Android und sind mit einem JVM-Unit-Test
 *   prüfbar.
 *
 * - AppNavigation erzeugt den Back-Stack und verändert ihn nur über
 *   backStack.navigateTo(...) und backStack.navigateBack().
 *
 * - appNavEntryProvider erhält alle Abhängigkeiten explizit als Parameter:
 *
 *      backStack      -> List<NavKey>, nur lesend, für den Guard gegen
 *                        verspätete Effekte
 *      onMessage      -> (String) -> Unit, statt des ganzen SnackbarController
 *      onError        -> (String) -> Unit
 *      onNavigateBack -> (PopReason) -> Unit
 *      onNavigateTo   -> (PersonKey) -> Unit
 *
 *   Der Provider kennt weder SnackbarController noch SnackbarHostState und
 *   verändert den Back-Stack nicht. Er reicht nur Verhalten an die Adapter
 *   durch.
 *
 * - Navigation und Meldungsausgabe bleiben bewusst getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *
 * - SnackbarController und der einzige sichtbare SnackbarHost liegen oberhalb
 *   des NavDisplay. Der CoroutineScope bleibt bei einem Destination-Wechsel bestehen.
 *   Auf der People-Liste erhält der Host Abstand zum FAB. Eine Meldung kann so
 *   nach erfolgreichem Speichern und NavigateBack sichtbar bleiben.
 *
 * - Ladefehler bleiben im jeweiligen Screen-State sichtbar; Retry startet
 *   einen neuen Ladeversuch. Fehler einzelner Aktionen nutzen ShowError.
 *
 * - Eine eigene Coordinator-Queue ist nicht erforderlich. SnackbarHostState
 *   serialisiert gleichzeitig angeforderte Snackbars bereits selbst.
 *
 * - Save und Cancel setzen unterschiedliche PopReason-Werte. Die vorhandenen
 *   Animationen machen damit die Art der Rücknavigation sichtbar.
 *   System-Back liest isSaving direkt aus dem ViewModel des aktuellen PersonKey
 *   und wartet während eines Speichervorgangs auf dessen Ergebnis.
 *   Verspätete Navigationseffekte ändern nur ihren aktuellen Backstack-Eintrag.
 *   Der Guard dafür sitzt im appNavEntryProvider.
 *
 * - ShowUndo ist bereits als Action-Snackbar vorbereitet. In diesem Schritt
 *   bleiben onAction und onDismiss bewusst leer. Kommt Undo hinzu, wächst die
 *   Parameterliste des Providers; dann ist ein Interface oder der
 *   SnackbarController als einzelner Parameter übersichtlicher.
 *
 * Lernziele:
 *
 * - Navigation 3 mit rememberNavBackStack, NavDisplay und NavKey einsetzen.
 * - Verantwortlichkeiten auf mehrere Dateien verteilen: UI-Rahmen,
 *   Key-Screen-Zuordnung und Stack-Operationen.
 * - Einen entryProvider in eine eigene Funktion auslagern und seine
 *   Abhängigkeiten explizit als Parameter übergeben.
 * - Function-Typen statt konkreter Klassen übergeben, damit eine Funktion nur
 *   das kennt, was sie tatsächlich braucht.
 * - Screenübergreifende Meldungen mit einem navigationweit lebenden
 *   SnackbarController darstellen.
 * - Navigation und globale UI-Meldungen als getrennte Verantwortlichkeiten
 *   verstehen.
 * - Vorhandene Material-3-Infrastruktur statt einer eigenen Message-Queue nutzen.
 * - Unterschiedliche Navigationsarten über Animationen sichtbar machen.
 */