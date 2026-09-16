package de.rogallab.mobile.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey

// Describes one item of the Material 3 NavigationBar.
interface ITopLevelNavItem : NavKey {
   val navKey: NavKey
   @get:StringRes val labelResourceId: Int
   val logLabel: String
   val iconActive: ImageVector
   val iconOutlined: ImageVector
}
