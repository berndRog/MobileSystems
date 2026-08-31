package de.rogallab.mobile.ui.news.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.ui.common.ArticleCard
import de.rogallab.mobile.ui.news.NewsIntent
import de.rogallab.mobile.ui.news.NewsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
   state: NewsUiState,
   contentPadding: PaddingValues,
   onIntent: (NewsIntent) -> Unit,
) {
   Column(
      modifier = Modifier
         .fillMaxSize()
         .padding(contentPadding),
   ) {
      TopAppBar(
         windowInsets = WindowInsets(0),
         title = { Text(stringResource(R.string.news_title)) },
      )

      OutlinedTextField(
         value = state.searchText,
         onValueChange = { value ->
            onIntent(NewsIntent.SearchTextChanged(value))
         },
         modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
         label = { Text(stringResource(R.string.search_hint)) },
         singleLine = true,
         trailingIcon = {
            IconButton(onClick = { onIntent(NewsIntent.Search) }) {
               Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = stringResource(R.string.action_search),
               )
            }
         },
         keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
         keyboardActions = KeyboardActions(
            onSearch = { onIntent(NewsIntent.Search) }
         ),
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
               ArticleCard(
                  article = article,
                  onClick = { onIntent(NewsIntent.Detail(article)) },
               )
            }
         }
      }
   }
}
