package de.rogallab.mobile.ui.navigation.composables

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.rogallab.mobile.ui.navigation.ArticlesList
import de.rogallab.mobile.ui.navigation.Nav3ViewModelTopLevel
import de.rogallab.mobile.ui.navigation.NewsList

@Composable
fun BottomNav3Bar(
   navViewModel: Nav3ViewModelTopLevel
) {
   val bottomNavItems = listOf(NewsList, ArticlesList)
   val currentKey = navViewModel.currentTopLevelKey

   NavigationBar {
      bottomNavItems.forEach { item ->
         val isSelected = item == currentKey
         NavigationBarItem(
            selected = item == currentKey,
            icon = {
               Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.title
               )
            },
            label = { Text(item.title) },
            onClick = {
               when (item) {
                  NewsList -> navViewModel.switchTopLevel(NewsList)
                  ArticlesList -> navViewModel.switchTopLevel(ArticlesList)
               }
            }
         )
      }
   }
}