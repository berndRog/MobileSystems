package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey

interface INavHandler {
   fun push(destination: NavKey)
   fun pop() // pop the last entry from the back stack
   fun popToRootAndNavigate(rootDestination: NavKey)
   // switches to a top-level
   fun switchTopLevel(key: NavKey)
}