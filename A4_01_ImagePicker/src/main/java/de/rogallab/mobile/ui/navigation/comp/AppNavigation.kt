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

   // Keep PeopleViewModel above NavDisplay. A4_01 retains the Swipe/Undo
   // behavior from A3_04, so pending visual removals must survive navigation.
   val peopleViewModel = koinViewModel<PeopleViewModel>()

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

            // Root destination: list of people. Swipe and Undo are deliberately
            // retained so A4_01 builds on A3_04 instead of replacing it.
            entry<PeopleKey> {

               PeopleAdapter(
                  viewModel = peopleViewModel,
                  modifier = Modifier
                     .padding(contentPadding)
                     .fillMaxSize(),

                  onMessage = snackbarController::showMessage,
                  onError = snackbarController::showError,

                  // A swipe delete first changes only the visible list. The
                  // Action Snackbar decides between Undo and repository commit.
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
                     currentPopReason = PopReason.Cancel
                     remove(backStack)
                  },

                  // null -> create, id -> detail/edit.
                  onNavigateTo = { personId ->
                     add(PersonKey(personId), backStack)
                  },
               )
            }

            // Shared destination for create and edit. The Person feature is the
            // new part of A4_01: it adds gallery/camera image selection and the
            // image edit lifecycle while keeping the existing navigation flow.
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
 * - A4_01_ImagePicker beginnt ein neues Kapitel, baut technisch aber bewusst
 *   vollständig auf A3_04_SwipeGestures auf. Navigation, Effects, Swipe,
 *   animateItem() und Undo bleiben erhalten. Neu ist ausschließlich der
 *   Bild-Lebenszyklus im Person-Feature.
 *
 * - Dadurch ist die Lernprogression kumulativ:
 *
 *      A3_03_Navigation
 *         -> Navigation 3, Effects und Navigationsanimationen
 *
 *      A3_04_SwipeGestures
 *         -> zusätzlich Swipe, Listenanimation und Undo
 *
 *      A4_01_ImagePicker
 *         -> zusätzlich Gallery/Camera und temporäre Bilddateien
 *
 * - PeopleViewModel lebt weiterhin oberhalb des NavDisplay, weil pending
 *   Removals zur bereits bekannten Swipe-/Undo-Funktion gehören. Das neue
 *   ImagePicker-Thema verändert diese Verantwortung nicht.
 *
 * - Der Person-Screen ergänzt dagegen einen eigenen Edit-Lebenszyklus für
 *   Bilder: Auswahl bzw. Kameraaufnahme verändern zunächst die laufende
 *   Edit-Session. Erst Save bestätigt die neuen Dateien; Cancel verwirft sie.
 *
 * - Navigation und Meldungsausgabe bleiben getrennt. Der SnackbarController
 *   liegt weiterhin oberhalb des NavDisplay und benötigt keinen Coordinator.
 *
 * Lernziele:
 *
 * - Ein neues Thema ergänzen, ohne bereits eingeführte Funktionen zurückzubauen.
 * - UI-/Navigation-State und den Lebenszyklus temporärer Dateien unterscheiden.
 * - Bildauswahl als delegierte Infrastruktur in einen bestehenden UDF-Fluss
 *   integrieren.
 * - Kommentare so formulieren, dass sie die aktuelle Verantwortung erklären
 *   und nicht von einer späteren Modulnummer abhängen.
 */
