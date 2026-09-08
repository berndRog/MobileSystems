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
               snackbarHostState = snackbarHostState,

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

               onNavigateBack = {
                  currentPopReason = PopReason.Cancel
                  remove(backStack)
               },

               // null -> create, id -> detail/edit.
               onNavigateTo = { personId ->
                  add(PersonKey(personId), backStack)
               },
            )
         }

         // Shared destination for create and edit. The new part of A4_01 is
         // gallery/camera image selection and the image edit lifecycle.
         entry<PersonKey> { personKey ->
            val personViewModel = koinViewModel<PersonViewModel> {
               parametersOf(personKey.personId)
            }

            PersonAdapter(
               viewModel = personViewModel,
               snackbarHostState = snackbarHostState,

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
   } else {
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
