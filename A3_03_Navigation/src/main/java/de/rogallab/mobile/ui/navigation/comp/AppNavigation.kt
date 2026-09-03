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

   // One SnackbarHostState is shared by all destinations.
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

      // Keeps the visible navigation animations from the previous project.
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
               snackbarHostState = snackbarHostState,

               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,

               onNavigateBack = {
                  currentPopReason = PopReason.Cancel
                  remove(backStack)
               },
               // null -> create, id -> detail/edit.
               onNavigateTo = { personId ->
                  add(
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
               snackbarHostState = snackbarHostState,

               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,

               onNavigateBack = { reason ->
                  currentPopReason = when (reason) {
                     BackReason.Save -> PopReason.SAVE
                     BackReason.Cancel -> PopReason.Cancel
                  }
                  remove(backStack)
               },
            )
         }
      },
   )
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
 * - A3_03_Navigation verwendet den standardmäßigen saveable Navigation-3-Back-Stack:
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
 * - ShowUndo ist bereits als Action-Snackbar vorbereitet. In diesem Schritt
 *   bleiben onAction und onDismiss bewusst leer.

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
