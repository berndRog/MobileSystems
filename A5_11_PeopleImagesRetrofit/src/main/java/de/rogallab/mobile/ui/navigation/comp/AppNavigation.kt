package de.rogallab.mobile.ui.navigation.comp

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

   // Reuse the Navigation 3 back stack introduced in the preceding examples.
   val backStack = rememberNavBackStack(PeopleKey)

   // One SnackbarHostState is shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   // The controller and its CoroutineScope live above NavDisplay. Therefore a
   // Snackbar started by one destination can remain active after navigation.
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   // Selects the visible pop animation for Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.Cancel) }

   val navEntryProvider = appNavEntryProvider(
      backStack = backStack,
      onMessage = snackbarController::showMessage,
      onError = snackbarController::showError,
      snackbarHostState = snackbarHostState,
      onConfirmRemove = { message, actionLabel, onAction ->
         snackbarController.showAction(message, actionLabel, onAction)
      },
      onNavigateBack = { reason ->
         currentPopReason = reason
         remove(backStack)
      },
      onNavigateTo = { destination -> add(destination, backStack) },
   )

   // Logs the initial or restored back stack.
   LaunchedEffect(backStack) {
      logNavigationOperation(
         operation = "initial",
         destination = backStack.lastOrNull(),
         backStack = backStack,
      )
   }

   NavDisplay(
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

      // Navigation behavior remains unchanged from chapter 3.
      transitionSpec = NavigationAnimations.enterTransitionSpec,
      popTransitionSpec =
         NavigationAnimations.popTransitionSpec(currentPopReason),
      predictivePopTransitionSpec =
         NavigationAnimations.predictivePopTransitionSpec,

      entryProvider = navEntryProvider,
   )
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
 *   Bilddateien. Save bestätigt die Edit-Session, Cancel verwirft sie.
 *
 * Lernziele:
 *
 * - ImagePicker und Swipe-to-Delete ohne Undo-Komplexität kombinieren.
 * - Eine destruktive Aktion mit einer Action-Snackbar bestätigen.
 * - Delete-Bestätigung und Undo als unterschiedliche Ausbaustufen verstehen.
 */
