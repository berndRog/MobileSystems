package de.rogallab.mobile.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.launch

class ArticleViewModel(
   val article: Article,
   private val _articleRepository: IArticleRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<ArticleEffect>,
) : ViewModel(), IEffectSource<ArticleEffect> by _effectDelegate {

   fun onIntent(intent: ArticleIntent) {
      when (intent) {
         ArticleIntent.Save -> save()
      }
   }

   private fun save() {
      viewModelScope.launch {
         _articleRepository.save(article)
            .onSuccess {
               _effectDelegate.emit(
                  ArticleEffect.ShowMessage(
                     _stringProvider.getString(R.string.message_article_saved)
                  )
               )
            }
            .onFailure {
               _effectDelegate.emit(
                  ArticleEffect.ShowError(
                     _stringProvider.getString(R.string.error_article_save)
                  )
               )
            }
      }
   }
}
