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

   // One SnackbarHostState and one visible host are shared by all destinations.
   val snackbarHostState = remember { SnackbarHostState() }
   // The controller and its CoroutineScope live above NavDisplay. Therefore a
   // Snackbar started by one destination can remain active after navigation.
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )

   // Selects the visible remove animation for Save or Cancel.
   var currentPopReason by remember { mutableStateOf(PopReason.Cancel) }

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

         entryProvider = entryProvider {

            // Root destination: list of people.
            entry<PeopleKey> { peopleKey ->
               val peopleViewModel = koinViewModel<PeopleViewModel>()

               PeopleAdapter(
                  viewModel = peopleViewModel,

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  onConfirmRemove = { message, actionLabel, personId ->
                     snackbarController.showAction(
                        message = message,
                        actionLabel = actionLabel,
                        onAction = {
                           peopleViewModel.onIntent(PeopleIntent.ConfirmRemove(personId))
                        },
                     )
                  },

                  onNavigateBack = {
                     currentPopReason = PopReason.Cancel
                     remove(backStack)
                  },
                  // null -> create, id -> detail/edit.
                  onNavigateTo = { personId ->
                     if (backStack.lastOrNull() === peopleKey) {
                        add(
                           destination = PersonKey(personId),
                           backStack = backStack,
                        )
                     }
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

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  onNavigateBack = { reason ->
                     if (backStack.lastOrNull() === personKey) {
                        currentPopReason = when (reason) {
                           BackReason.Save -> PopReason.SAVE
                           BackReason.Cancel -> PopReason.Cancel
                        }
                        remove(backStack)
                     }
                  },
               )
            }
         },
      )
   }
}

// Adds a destination to the standard Navigation 3 back stack.
private fun add(
   destination: NavKey,
   backStack: MutableList<NavKey>,
) {
   // put at the end of the list, so that it becomes the current destination.
   backStack.add(destination)

   logNavigationOperation("add", destination, backStack)
}

// Removes the current destination while preserving the root destination.
private fun remove(
   backStack: MutableList<NavKey>,
) {
   if (backStack.size > 1) {
      // remove the last destination, so that the previous one becomes the current destination.
      val removedDestination = backStack.removeLastOrNull()
      logNavigationOperation("pop", removedDestination, backStack)
   } else {
      logNavigationOperation(
         "pop ignored - root remains", backStack.lastOrNull(), backStack)
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
 * - A4_01_ImagePicker baut auf Navigation und Swipe-Gesten auf, übernimmt aber
 *   bewusst nicht die zusätzliche Undo-Mechanik. Neu ist in diesem Schritt der
 *   Bild-Lebenszyklus im Person-Feature.
 *
 * - Ein navigationweit lebender SnackbarHost mit imePadding() zeigt Meldungen
 *   auch nach einem Wechsel des Navigation-3-Backstacks weiter an.
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
