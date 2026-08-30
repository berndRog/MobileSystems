package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import de.rogallab.mobile.ui.navigation.CarListKey
import de.rogallab.mobile.ui.navigation.CarsTopLevel
import de.rogallab.mobile.ui.navigation.ITopLevelNavItem
import de.rogallab.mobile.ui.navigation.PeopleTopLevel
import de.rogallab.mobile.ui.navigation.PersonListKey
import de.rogallab.mobile.ui.navigation.TDrivesKey
import de.rogallab.mobile.ui.navigation.TDrivesTopLevel
import de.rogallab.mobile.ui.navigation.topLevelNavItems

// Creates the saveable navigation state for the three top-level sections.
//
// Every top-level item receives its own standard Navigation 3 back stack.
// rememberNavBackStack preserves the keys across configuration changes and
// process recreation because all navigation keys implement NavKey and are
// serializable.
@Composable
fun rememberAppNavigationState(): AppNavigationState {
   // Creates one independent standard Navigation 3 back stack for each
   // top-level section. The explicit variables make the three stacks easy to
   // inspect in the debugger and keep the example approachable for students.
   val personBackStack: NavBackStack<NavKey> =
      rememberNavBackStack(PersonListKey)
   val carBackStack: NavBackStack<NavKey> =
      rememberNavBackStack(CarListKey)
   val testDriveBackStack: NavBackStack<NavKey> =
      rememberNavBackStack(TDrivesKey)

   // The selected index is saveable because it is a simple Int value.
   // Index 0 represents People, the start section of the application.
   val selectedTopLevelIndexState = rememberSaveable {
      mutableStateOf(0)
   }

   // The map connects the UI metadata of each NavigationBar item with the
   // standard back stack that belongs to that section.
   val backStacks: Map<ITopLevelNavItem, NavBackStack<NavKey>> = remember(
      personBackStack,
      carBackStack,
      testDriveBackStack,
   ) {
      linkedMapOf(
         PeopleTopLevel to personBackStack,
         CarsTopLevel to carBackStack,
         TDrivesTopLevel to testDriveBackStack,
      )
   }

   return remember(
      selectedTopLevelIndexState,
      backStacks,
   ) {
      AppNavigationState(
         startTopLevelNavItem = PeopleTopLevel,
         navItems = topLevelNavItems,
         selectedTopLevelIndexState = selectedTopLevelIndexState,
         backStacks = backStacks,
      )
   }
}

// Holds navigation state but does not execute navigation operations.
//
// AppNavigator is the only class that changes the selected top-level item or
// one of the back stacks. This separation keeps state and operations explicit.
class AppNavigationState(
   val startTopLevelNavItem: ITopLevelNavItem,
   val navItems: List<ITopLevelNavItem>,
   private val selectedTopLevelIndexState: MutableState<Int>,
   val backStacks: Map<ITopLevelNavItem, NavBackStack<NavKey>>,
) {
   var currentTopLevelNavItem: ITopLevelNavItem
      get() = navItems[selectedTopLevelIndexState.value]
      set(topLevelNavItem) {
         val selectedIndex = navItems.indexOf(topLevelNavItem)
         require(selectedIndex >= 0) {
            "The selected top-level item must be contained in navItems."
         }
         selectedTopLevelIndexState.value = selectedIndex
      }

   // Returns the back stack that belongs to a specific top-level item.
   fun backStackFor(
      topLevelNavItem: ITopLevelNavItem,
   ): NavBackStack<NavKey> =
      backStacks.getValue(topLevelNavItem)

   // Returns the back stack of the currently selected section.
   fun currentBackStack(): NavBackStack<NavKey> =
      backStackFor(currentTopLevelNavItem)

   // Decorates every back stack independently.
   //
   // Inactive sections are not displayed, but their decorated entries remain
   // retained. This preserves rememberSaveable values and entry-scoped
   // ViewModels while the user switches between bottom-navigation sections.
   @Composable
   fun toDecoratedEntries(
      entryProvider: (NavKey) -> NavEntry<NavKey>,
   ): List<NavEntry<NavKey>> {
      val decoratedEntriesByTopLevel =
         backStacks.mapValues { (_, backStack) ->
            rememberDecoratedNavEntries(
               backStack = backStack,
               entryDecorators = listOf(
                  rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                  rememberViewModelStoreNavEntryDecorator<NavKey>(),
               ),
               entryProvider = entryProvider,
            )
         }

      val startEntries =
         decoratedEntriesByTopLevel[startTopLevelNavItem].orEmpty()

      // People is always the first stack in the flattened entry list.
      // This implements the "exit through home" pattern used by the official
      // Navigation 3 multiple-back-stacks recipe.
      return if (currentTopLevelNavItem == startTopLevelNavItem) {
         startEntries
      }
      else {
         startEntries +
            decoratedEntriesByTopLevel[currentTopLevelNavItem].orEmpty()
      }
   }
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - rememberNavBackStack() erzeugt je Hauptbereich einen standardmäßigen,
//   speicherbaren Navigation-3-Back-Stack.
// - AppNavigationState hält ausschließlich Zustand und verändert ihn nicht
//   selbst; Navigationsoperationen liegen im AppNavigator.
// - Der ausgewählte Hauptbereich wird getrennt von den drei Back Stacks
//   gespeichert.
// - Die Decorators erhalten Compose-Zustand und entry-spezifische ViewModels
//   auch dann, wenn ein Hauptbereich momentan nicht sichtbar ist.
