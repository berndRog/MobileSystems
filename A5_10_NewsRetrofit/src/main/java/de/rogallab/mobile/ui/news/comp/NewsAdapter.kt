package de.rogallab.mobile.ui.news.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.news.NewsEffect
import de.rogallab.mobile.ui.news.NewsViewModel

@Composable
fun NewsAdapter(
   viewModel: NewsViewModel,
   contentPadding: PaddingValues,
   onError: (String) -> Unit,
   onNavigateToArticle: (Article) -> Unit,
) {
   val state by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is NewsEffect.ShowError -> onError(effect.message)
         is NewsEffect.NavigateToArticle -> onNavigateToArticle(effect.article)
      }
   }

   NewsScreen(
      state = state,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
