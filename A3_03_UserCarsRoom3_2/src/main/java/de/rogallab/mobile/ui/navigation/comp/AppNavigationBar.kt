package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import de.rogallab.mobile.ui.navigation.ITopLevelNavItem
import kotlin.collections.forEach

@Composable
fun AppBottomNavigationBar(
   navItems: List<ITopLevelNavItem>,
   currentTopLevelNavItem: ITopLevelNavItem,
   onTopLevelSelected: (ITopLevelNavItem) -> Unit,
) {
   NavigationBar {
      navItems.forEach { item ->
         val selected = item == currentTopLevelNavItem
         val label = stringResource(item.labelResourceId)
         NavigationBarItem(
            selected = selected,
            onClick = { onTopLevelSelected(item) },
            icon = {
               Icon(
                  imageVector = if (selected) item.iconActive else item.iconOutlined,
                  contentDescription = label,
               )
            },
            label = { Text(label) },
         )
      }
   }
}