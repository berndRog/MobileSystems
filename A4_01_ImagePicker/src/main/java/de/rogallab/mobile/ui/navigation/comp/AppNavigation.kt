package de.rogallab.mobile.ui.navigation.comp

import de.rogallab.mobile.AppTiming
import org.koin.compose.koinInject
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
fun AppNavigation(timing: AppTiming = koinInject()) {

   val navigationAnimations = remember(timing.animationDuration) {
      NavigationAnimations(timing.animationDuration)
   }
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(TAG, "Composition #${nComp.intValue++}") }

   // Creates the standard saveable Navigation 3 back stack.
   val backStack = rememberNavBackStack(PeopleKey)

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
      onConfirmRemove = { message, actionLabel, onAction ->
         snackbarController.showAction(message, actionLabel, onAction)
      },
      onNavigateBack = { reason ->
         currentPopReason = reason
         remove(backStack)
      },
      onNavigateTo = { destination -> add(destination, backStack) },
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
         transitionSpec = navigationAnimations.enterTransitionSpec,
         popTransitionSpec =
            navigationAnimations.popTransitionSpec(currentPopReason),
         predictivePopTransitionSpec =
            navigationAnimations.predictivePopTransitionSpec,

         entryProvider = navEntryProvider,
      )
   }
}

@Composable
private fun calculateSnackbarBottomPadding(showListFab: Boolean): Dp {
   val listFabSnackbarClearance = 88.dp // 56 dp FAB + 16 dp edge + 16 dp gap

   // Scaffold already keeps the snackbar above the system bar. Add the larger
   // of the remaining IME height and the clearance required for the list FAB.
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
 * - Die Navigation ist wie in A3_03 auf drei Dateien verteilt:
 *   AppNavigation hält Backstack und UI-Rahmen, AppNavEntryProvider ordnet
 *   NavKeys den Screens zu, BackStackOperations enthält add/remove und Logging.
 *   Zusätzliche Screen-Aktionen werden als Callback an den Provider übergeben.
 *
 * - A4_01_ImagePicker baut auf Navigation und Swipe-Gesten auf, übernimmt aber
 *   bewusst nicht die zusätzliche Undo-Mechanik. Neu ist in diesem Schritt der
 *   Bild-Lebenszyklus im Person-Feature.
 *
 * - Ein navigationweit lebender SnackbarHost zeigt Meldungen auch nach einem
 *   Wechsel des Navigation-3-Backstacks weiter an. Sein unterer Abstand hält
 *   die Snackbar oberhalb der Tastatur und des Listen-FAB.
 *   Save und Cancel behalten unterschiedliche Pop-Animationen.
 *
 * - Navigation und Meldungsausgabe bleiben getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *      ConfirmRemove             -> Action-Snackbar
 *
 * - Swipe-to-Delete verändert die sichtbare Liste nicht vorzeitig. Das ViewModel
 *   erzeugt ConfirmRemove; nur die Action der Snackbar führt anschließend zu
 *   PeopleIntent.ConfirmRemove und damit zur Repository-Operation.
 *
 * - Wird die Snackbar verworfen oder läuft sie ab, bleibt das Repository
 *   unverändert. Deshalb werden weder VisualRemovalDelegate noch pending
 *   Removals oder ein Restore-State benötigt.
 *
 * - PeopleViewModel kann wieder im PeopleKey-Eintrag erzeugt werden. Erst
 *   A4_02_ImagePickerUndo benötigt den länger lebenden temporären Removal-State.
 *
 * - Der Person-Screen ergänzt unabhängig davon Gallery/Camera und temporäre
 *   Bilddateien. Save bestätigt die Edit-Session, Cancel und System-Back
 *   verwerfen sie.
 *
 * Lernziele:
 *
 * - ImagePicker und Swipe-to-Delete ohne Undo-Komplexität kombinieren.
 * - Eine destruktive Aktion mit einer Action-Snackbar bestätigen.
 * - Delete-Bestätigung und Undo als unterschiedliche Ausbaustufen verstehen.
 */
