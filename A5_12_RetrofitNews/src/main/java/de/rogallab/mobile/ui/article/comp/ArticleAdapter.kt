package de.rogallab.mobile.ui.article.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.article.ArticleEffect
import de.rogallab.mobile.ui.article.ArticleViewModel

@Composable
fun ArticleAdapter(
   viewModel: ArticleViewModel,
   allowSave: Boolean,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
) {
   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is ArticleEffect.ShowMessage -> onMessage(effect.message)
         is ArticleEffect.ShowError -> onError(effect.message)
      }
   }

   ArticleScreen(
      article = viewModel.article,
      allowSave = allowSave,
      contentPadding = contentPadding,
      onBack = onBack,
      onIntent = viewModel::onIntent,
   )
}
