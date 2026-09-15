package de.rogallab.mobile.ui.navigation.comp

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.article.ArticleViewModel
import de.rogallab.mobile.ui.article.comp.ArticleAdapter
import de.rogallab.mobile.ui.articles.ArticlesIntent
import de.rogallab.mobile.ui.articles.ArticlesViewModel
import de.rogallab.mobile.ui.articles.comp.ArticlesAdapter
import de.rogallab.mobile.ui.navigation.ArticleKey
import de.rogallab.mobile.ui.navigation.ArticlesKey
import de.rogallab.mobile.ui.navigation.NewsKey
import de.rogallab.mobile.ui.news.NewsViewModel
import de.rogallab.mobile.ui.news.comp.NewsAdapter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation() {
   val activity = LocalActivity.current
   val newsBackStack = rememberNavBackStack(NewsKey)
   val articlesBackStack = rememberNavBackStack(ArticlesKey)
   var selectedTab by rememberSaveable { mutableIntStateOf(0) }

   val backStack = if (selectedTab == 0) newsBackStack else articlesBackStack
   val snackbarHostState = remember { SnackbarHostState() }
   val snackbarController = rememberSnackbarController(snackbarHostState)
   val showBottomBar = backStack.lastOrNull() !is ArticleKey

   Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      bottomBar = {
         if (showBottomBar) {
            NavigationBar {
               NavigationBarItem(
                  selected = selectedTab == 0,
                  onClick = { selectedTab = 0 },
                  icon = {
                     Icon(
                        imageVector = if (selectedTab == 0) Icons.Filled.Search
                                      else Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.nav_news),
                     )
                  },
                  label = { Text(stringResource(R.string.nav_news)) },
               )
               NavigationBarItem(
                  selected = selectedTab == 1,
                  onClick = { selectedTab = 1 },
                  icon = {
                     Icon(
                        imageVector = if (selectedTab == 1) Icons.Filled.Bookmarks
                                      else Icons.Outlined.Bookmarks,
                        contentDescription = stringResource(R.string.nav_saved),
                     )
                  },
                  label = { Text(stringResource(R.string.nav_saved)) },
               )
            }
         }
      },
   ) { contentPadding ->
      val appEntryProvider = entryProvider {
         entry<NewsKey> {
            val viewModel = koinViewModel<NewsViewModel>()
            NewsAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onError = snackbarController::showError,
               onNavigateToArticle = { article ->
                  newsBackStack.add(ArticleKey(article = article, allowSave = true))
               },
            )
         }
         entry<ArticlesKey> {
            val viewModel = koinViewModel<ArticlesViewModel>()
            ArticlesAdapter(
               viewModel = viewModel,
               contentPadding = contentPadding,
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onConfirmRemove = { message, actionLabel, url ->
                  snackbarController.showAction(
                     message = message,
                     actionLabel = actionLabel,
                     onAction = {
                        viewModel.onIntent(ArticlesIntent.ConfirmRemove(url))
                     },
                  )
               },
               onNavigateToArticle = { article ->
                  articlesBackStack.add(ArticleKey(article = article, allowSave = false))
               },
            )
         }
         entry<ArticleKey> { key ->
            val viewModel = koinViewModel<ArticleViewModel> {
               parametersOf(key.article)
            }
            ArticleAdapter(
               viewModel = viewModel,
               allowSave = key.allowSave,
               contentPadding = contentPadding,
               onBack = { backStack.removeLastOrNull() },
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
            )
         }
      }

      NavDisplay(
         backStack = backStack,
         onBack = {
            if (backStack.size > 1) {
               backStack.removeLastOrNull()
            }
            else {
               activity?.finish()
            }
         },
         entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(
               rememberSaveableStateHolder()
            ),
            rememberViewModelStoreNavEntryDecorator(),
         ),
         entryProvider = appEntryProvider,
         modifier = Modifier.fillMaxSize(),
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - News und gespeicherte Artikel besitzen jeweils einen eigenen Navigation-3-Back-Stack.
 * - Jeder NavEntry besitzt einen eigenen ViewModelStore. Dadurch erhält jeder Artikel
 *   sein eigenes ArticleViewModel mit dem zum ArticleKey gehörenden Artikel.
 * - Navigation bleibt in AppNavigation; ViewModels erzeugen nur Effects.
 * - SnackbarController ist die einzige Snackbar-Infrastruktur der Anwendung.
 */
