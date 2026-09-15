package de.rogallab.mobile.ui.articles.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.ui.articles.ArticlesIntent
import de.rogallab.mobile.ui.articles.ArticlesUiState
import de.rogallab.mobile.ui.common.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
   state: ArticlesUiState,
   contentPadding: PaddingValues,
   onIntent: (ArticlesIntent) -> Unit,
) {
   val detailDescription = stringResource(R.string.article_open)
   val deleteDescription = stringResource(R.string.article_delete)

   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      TopAppBar(
         windowInsets = WindowInsets(0),
         title = { Text(stringResource(R.string.articles_title)) },
      )

      if (state.isLoading) {
         Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            CircularProgressIndicator()
         }
      }
      else {
         LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
               start = 16.dp,
               top = 8.dp,
               end = 16.dp,
               bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
         ) {
            items(
               items = state.articles,
               key = { article -> article.url },
            ) { article ->
               SwipeCard(
                  onDetail = { onIntent(ArticlesIntent.Detail(article)) },
                  onDelete = { onIntent(ArticlesIntent.RequestRemove(article.url)) },
                  detailContentDescription = detailDescription,
                  deleteContentDescription = deleteDescription,
               ) {
                  ArticleCard(
                     article = article,
                     onClick = { onIntent(ArticlesIntent.Detail(article)) },
                  )
               }
            }
         }
      }
   }
}
