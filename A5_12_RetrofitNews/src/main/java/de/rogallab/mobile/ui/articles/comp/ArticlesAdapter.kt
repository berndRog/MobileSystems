package de.rogallab.mobile.ui.articles.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.articles.ArticlesEffect
import de.rogallab.mobile.ui.articles.ArticlesViewModel

@Composable
fun ArticlesAdapter(
   viewModel: ArticlesViewModel,
   contentPadding: PaddingValues,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, String) -> Unit,
   onNavigateToArticle: (Article) -> Unit,
) {
   val state by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is ArticlesEffect.ShowMessage -> onMessage(effect.message)
         is ArticlesEffect.ShowError -> onError(effect.message)
         is ArticlesEffect.ConfirmRemove -> onConfirmRemove(
            effect.message,
            effect.actionLabel,
            effect.url,
         )
         is ArticlesEffect.NavigateToArticle -> onNavigateToArticle(effect.article)
      }
   }

   ArticlesScreen(
      state = state,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
