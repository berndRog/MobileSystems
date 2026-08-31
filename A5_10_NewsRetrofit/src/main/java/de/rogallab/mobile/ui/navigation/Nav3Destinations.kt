package de.rogallab.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

// Navigation destinations as NavKeys
@Serializable
data object NewsList : NavKey, ITopLevelNavItem {
   override val title = "Suchen"
   override val unselectedIcon: ImageVector = Icons.Outlined.Search
   override val selectedIcon: ImageVector = Icons.Filled.Search
}

@Serializable
data object ArticleWeb : NavKey, INavItem {
   override val title = "Anzeigen"
}

@Serializable
data object ArticlesList: NavKey, ITopLevelNavItem {
    override val title = "Gespeichert"
    override val unselectedIcon: ImageVector = Icons.Outlined.People
    override val selectedIcon: ImageVector = Icons.Filled.People
}

