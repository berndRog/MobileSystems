package de.rogallab.mobile.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.utilities.logDebug

/**
 * Nav3ViewModelTopLevel
 *
 * ViewModel for Bottom Navigation with independent back stacks per tab.
 *
 * Concepts:
 * - Each top-level tab has its own independent navigation stack
 * - Tabs retain their navigation history when switching
 * - Efficient state management via SnapshotStateList for direct Compose integration
 *
 * @param startDestination The start tab (default: PeopleList)
 */
class Nav3ViewModelTopLevel(
   private val startDestination: NavKey = NewsList
) : ViewModel(), INavHandler {
                     // 12345678901234567890123
   private val tag = "<-Nav3ViewModelTopLevel"
   init { logDebug(tag, "init instance=${System.identityHashCode(this)}") }

   // Map: Top-Level NavKey -> associated navigation stack
   private val _topLevelBackStacks: MutableMap<NavKey, SnapshotStateList<NavKey>> =
      mutableMapOf(startDestination to mutableStateListOf(startDestination))

   // Currently active top-level tab
   var currentTopLevelKey: NavKey by mutableStateOf(startDestination)
      private set

   // The active stack observed by the UI
   val currentBackStack: SnapshotStateList<NavKey>
      get() = _topLevelBackStacks.getOrPut(currentTopLevelKey) {
         mutableStateListOf(currentTopLevelKey)
      }

   // Optional: Immutable list for external access
   val currentBackStackAsList: List<NavKey>
      get() = currentBackStack.toList()

   /**
    * Switches to another top-level tab.
    * Creates a new stack if necessary.
    *
    * @param key The target tab
    */
   override fun switchTopLevel(key: NavKey) {
      if (key != currentTopLevelKey) {
         currentTopLevelKey = key
         logDebug(tag, "switchTopLevel -> $key")
         debugDump()
      } else {
         logDebug(tag, "switchTopLevel: already on $key")
      }
   }

   /**
    * Navigates to a new destination in the current tab stack.
    *
    * @param destination The target destination
    */
   override fun push(destination: NavKey) {
      currentBackStack.add(destination)
      logDebug(
         tag,
         "push: $destination -> Stack($currentTopLevelKey): ${currentBackStack.joinToString()}"
      )
      debugDump()
   }

   /**
    * Removes the topmost destination from the current stack.
    * If at root, nothing is removed (system back takes over).
    */
   override fun pop() {
      if (currentBackStack.size > 1) {
         val removed = currentBackStack.removeAt(currentBackStack.lastIndex)
         logDebug(tag,"pop: removed $removed -> Stack($currentTopLevelKey): ${currentBackStack.joinToString()}")
         debugDump()
      } else {
         logDebug(tag, "pop: at root of $currentTopLevelKey")
      }
   }

   /**
    * Switches to a root tab and resets its stack.
    *
    * @param rootDestination The target tab
    */
   override fun popToRootAndNavigate(rootDestination: NavKey) {
      switchTopLevel(rootDestination)
      currentBackStack.clear()
      currentBackStack.add(rootDestination)
      logDebug(tag, "popToRootAndNavigate -> $rootDestination")
      debugDump()
   }

   /**
    * Debug output: Shows all stacks.
    */
   private fun debugDump() {
      logDebug(tag, "=== Navigation State ===")
      logDebug(tag, "Current Top-Level: $currentTopLevelKey")
      _topLevelBackStacks.forEach { (key, stack) ->
         val marker = if (key == currentTopLevelKey) ">>> " else "    "
         logDebug(tag, "$marker[$key] = ${stack.joinToString(prefix = "[", postfix = "]")}")
      }
      logDebug(tag, "=======================")
   }
}
