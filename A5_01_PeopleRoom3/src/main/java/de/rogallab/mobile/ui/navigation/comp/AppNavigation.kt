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

                  // A swipe delete requests confirmation first. Only selecting
                  // the Snackbar action triggers the repository deletion.
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

                  onBack = {
                     currentPopReason = PopReason.Cancel
                     remove(backStack)
                  },

                  // null -> create, id -> detail/edit.
                  onNavigateTo = { personId ->
                     add(PersonKey(personId), backStack)
                  },
               )
            }

            // Shared destination for create and edit. Image selection and the
            // image edit lifecycle stay unchanged from A4_01.
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

                  onNavigateBack = { reason ->
                     currentPopReason = when (reason) {
                        BackReason.Save -> PopReason.Save
                        BackReason.Cancel -> PopReason.Cancel
                     }
                     remove(backStack)
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
   backStack.add(destination)
   logNavigationOperation("push", destination, backStack)
}

// Removes the current destination while preserving the root destination.
private fun remove(
   backStack: MutableList<NavKey>,
) {
   if (backStack.size > 1) {
      val removedDestination = backStack.removeLastOrNull()
      logNavigationOperation("remove", removedDestination, backStack)
   }
   else {
      logNavigationOperation(
         "pop ignored - root remains",
         backStack.lastOrNull(),
         backStack,
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
 * - A5_01_PeopleRoom3 übernimmt Navigation, Effects, ImagePicker und die einfache
 *   Löschbestätigung unverändert aus A4_01. Der neue Lernschritt liegt unterhalb
 *   der UI in der lokalen Room-3-Persistenzschicht.
 *
 * - Navigation und Meldungsausgabe bleiben getrennt:
 *
 *      NavigateTo / NavigateBack -> Navigation-3-Back-Stack
 *      ShowMessage / ShowError   -> SnackbarController
 *      ConfirmRemove             -> Action-Snackbar
 *
 * - Swipe-to-Delete verändert die sichtbare Liste nicht vorzeitig. Das ViewModel
 *   erzeugt ConfirmRemove; nur die Action der Snackbar führt zu
 *   PeopleIntent.ConfirmRemove und anschließend zur Repository-Operation.
 *
 * - AppNavigation kennt weder AppDatabase noch IPersonDao. Die UI arbeitet nur
 *   mit ViewModels und deren Domain-Repository-Schnittstelle. Dadurch bleibt der
 *   Wechsel von der bisherigen Persistenz auf den lokalen Room-3-Code unsichtbar
 *   für die Navigation.
 *
 * - A5_01 verwendet bewusst nicht den Undo-Zustand aus A4_02. Damit bleibt Room 3
 *   das zentrale neue Thema dieses Schritts.
 *
 * Lernziele:
 *
 * - UI-/Navigationsarchitektur beim Wechsel der Persistenz stabil halten.
 * - SnackbarController statt Coordinator für Meldungen und Bestätigung einsetzen.
 * - Repository als Trennlinie zwischen UI und Room erkennen.
 */
