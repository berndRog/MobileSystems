package de.rogallab.mobile.ui.features.article.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import coil.ImageLoader
import de.rogallab.mobile.R
import de.rogallab.mobile.data.dtos.Article
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.ui.errors.ErrorHandler
import de.rogallab.mobile.ui.features.article.ArticleIntent
import de.rogallab.mobile.ui.features.article.ArticlesViewModel
import de.rogallab.mobile.ui.features.news.composables.NewsItem
import de.rogallab.mobile.ui.navigation.Nav3ViewModelTopLevel
import de.rogallab.mobile.ui.navigation.NewsList
import de.rogallab.mobile.ui.navigation.composables.BottomNav3Bar
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalComposeUiApi
@Composable
fun ArticlesListScreen(
   navViewModel: Nav3ViewModelTopLevel = koinActivityViewModel<Nav3ViewModelTopLevel> { parametersOf(NewsList) },
   viewModel: ArticlesViewModel = koinActivityViewModel<ArticlesViewModel> { parametersOf(navViewModel) },
   imageLoader: ImageLoader = koinInject<ImageLoader>(),
   onNavigateTopLevel: (NavKey) -> Unit
) {
   val tag = "<-ArticlesListScreen"

   val articlesUiState by viewModel.articlesUiStateFlow.collectAsStateWithLifecycle()

   val snackbarHostState = remember { SnackbarHostState() }
   Scaffold(
      contentColor = MaterialTheme.colorScheme.onBackground,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
         TopAppBar(
            title = { Text(stringResource(R.string.savedarticles)) },
            navigationIcon = {
               IconButton(
                  onClick = { onNavigateTopLevel(NewsList) }
               ) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Default.ArrowBack,
                     contentDescription = stringResource(R.string.back)
                  )
               }
            }
         )
      },
      bottomBar = {
         BottomNav3Bar(navViewModel)
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(snackbarData = data)
         }
      },
      modifier = Modifier.fillMaxSize()
   ) { paddingValues ->
      Column(modifier = Modifier
         .padding(paddingValues = paddingValues)
         .padding(horizontal = 16.dp)) {

         if (articlesUiState.loading) {
            SideEffect { logDebug(tag, "Loading...") }
            Column(
               modifier = Modifier.fillMaxSize(),
               verticalArrangement = Arrangement.Center,
               horizontalAlignment = Alignment.CenterHorizontally
            ) {
               CircularProgressIndicator()
            }
         }
         else if (articlesUiState.articles != null) {
            SideEffect { logDebug(tag, "Articles loaded:${articlesUiState.articles?.size}") }

            val sortedArticles = articlesUiState.articles?.sortedBy { it.id }?.reversed()
               ?: emptyList()

            LazyColumn(
               state = rememberLazyListState()
            ) {
               items(
                  items = sortedArticles,
                  key = { it: Article -> it.id!! },
               ) { article ->

                  SwipeArticleWithoutUndoListItem(
                     article = article,                        // item
                     onNavigate = {  viewModel.onProcessIntent(
                        ArticleIntent.ShowWebArticle(false, article)) },
                     // navigate to DetailScreen
                     onRemove = {                     // remove item
                        viewModel.onProcessIntent(ArticleIntent.RemoveArticle(article))
                     }
                  ) {
                     NewsItem(
                        article,
                        onClick = { viewModel.onProcessIntent(
                           ArticleIntent.ShowWebArticle(false, article))  },
                        imageLoader = imageLoader
                     )
                  }
               }
            }
         }
      }
   } // Scaffold

   ErrorHandler(
      viewModel = viewModel,
      snackbarHostState = snackbarHostState
   )
}