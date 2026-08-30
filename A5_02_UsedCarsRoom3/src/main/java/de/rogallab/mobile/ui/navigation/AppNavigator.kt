package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.navigation.comp.AppNavigationState

private const val TAG = "<-AppNavigator"

// Executes all navigation operations by changing AppNavigationState.
//
// The class contains no Compose code and no Android ViewModel. It is created
// with remember() next to the navigation state and logs every operation so the
// three independent back stacks can be followed in Logcat.
class AppNavigator(
   private val navigationState: AppNavigationState,
) {
   init {
      logNavigationOperation(
         operation = "initial",
         destination = navigationState.currentBackStack().lastOrNull(),
      )
   }

   // Switches the visible top-level section without clearing its back stack.
   fun switchTopLevel(
      topLevelNavItem: ITopLevelNavItem,
   ) {
      if (topLevelNavItem == navigationState.currentTopLevelNavItem) {
         logNavigationOperation(
            operation = "switch ignored - already selected",
            destination = topLevelNavItem.navKey,
         )
         return
      }

      navigationState.currentTopLevelNavItem = topLevelNavItem

      logNavigationOperation(
         operation = "switch top-level",
         destination = topLevelNavItem.navKey,
      )
   }

   // Adds a destination to the back stack of the selected top-level section.
   fun push(destination: NavKey) {
      navigationState.currentBackStack().add(destination)

      logNavigationOperation(
         operation = "push",
         destination = destination,
      )
   }

   // Removes the current destination from the selected back stack.
   //
   // At the root of Cars or TestDrives, Back returns to People. At the
   // root of People, the navigation state remains unchanged.
   fun pop() {
      val currentBackStack = navigationState.currentBackStack()

      if (currentBackStack.size > 1) {
         val removedDestination = currentBackStack.removeLastOrNull()

         logNavigationOperation(
            operation = "pop",
            destination = removedDestination,
         )
      }
      else if (
         navigationState.currentTopLevelNavItem !=
         navigationState.startTopLevelNavItem
      ) {
         val previousTopLevelNavItem =
            navigationState.currentTopLevelNavItem

         navigationState.currentTopLevelNavItem =
            navigationState.startTopLevelNavItem

         logNavigationOperation(
            operation = "pop to start section",
            destination = previousTopLevelNavItem.navKey,
         )
      }
      else {
         logNavigationOperation(
            operation = "pop ignored - app root remains",
            destination = currentBackStack.lastOrNull(),
         )
      }
   }

   // Logs the current section and all three back stacks after every operation.
   // This method never changes navigation state.
   private fun logNavigationOperation(
      operation: String,
      destination: NavKey?,
   ) {
      val destinationText = destination?.toString() ?: "none"

      val stacksText = navigationState.navItems.joinToString(
         separator = " | ",
      ) { topLevelNavItem ->
         val selectionMarker =
            if (topLevelNavItem == navigationState.currentTopLevelNavItem) {
               ">>>"
            }
            else {
               "   "
            }

         val backStackText = navigationState
            .backStackFor(topLevelNavItem)
            .joinToString(
               separator = " -> ",
               prefix = "[",
               postfix = "]",
            )

         "$selectionMarker ${topLevelNavItem.logLabel}=$backStackText"
      }

      AppLogger.debug(
         tag = TAG,
         message = "$operation: $destinationText | $stacksText",
      )
   }
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - switchTopLevel(), push() und pop() bilden die Navigationsoperationen
//   ausdrücklich ab, ohne ein eigenes Navigation-ViewModel einzuführen.
// - Jede Operation verändert nur den ausgewählten Back Stack oder den
//   ausgewählten Hauptbereich.
// - Die Log-Ausgabe zeigt nach jeder Operation alle drei Back Stacks und
//   markiert den momentan sichtbaren Hauptbereich mit >>>.
// - People ist das Startziel; Cars und TestDrives führen bei Back auf
//   ihrer Wurzel zunächst zu People zurück.
