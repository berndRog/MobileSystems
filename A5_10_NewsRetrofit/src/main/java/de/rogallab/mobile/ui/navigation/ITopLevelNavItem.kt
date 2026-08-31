package de.rogallab.mobile.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector

sealed interface ITopLevelNavItem {
   val title: String
   val selectedIcon: ImageVector
   val unselectedIcon: ImageVector
}