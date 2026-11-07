package de.rogallab.mobile.ui.navigation.comp

import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.shared.domain.utilities.Alog

private const val TAG = "<-AppNavigation"

// Adds a destination to the standard Navigation 3 back stack.
internal fun add(
   destination: NavKey,
   backStack: MutableList<NavKey>,
) {
   backStack.add(destination)
   logNavigationOperation("push", destination, backStack)
}

// Removes the current destination while preserving the root destination.
internal fun remove(
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
internal fun logNavigationOperation(
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
