package de.rogallab.mobile.ui.articles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticlesViewModel(
   private val _articleRepository: IArticleRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<ArticlesEffect>,
) : ViewModel(), IEffectSource<ArticlesEffect> by _effectDelegate {

   private val _stateFlow = MutableStateFlow(ArticlesUiState())
   val stateFlow: StateFlow<ArticlesUiState> = _stateFlow.asStateFlow()

   init {
      observeArticles()
   }

   fun onIntent(intent: ArticlesIntent) {
      when (intent) {
         is ArticlesIntent.Detail -> {
            viewModelScope.launch {
               _effectDelegate.emit(ArticlesEffect.NavigateToArticle(intent.article))
            }
         } //navigateToArticle(intent.article)
         is ArticlesIntent.RequestRemove -> requestRemove(intent.url)
         is ArticlesIntent.ConfirmRemove -> remove(intent.url)
      }
   }

   private fun observeArticles() {
      viewModelScope.launch {
         _articleRepository.observeAll().collect { result ->
            result.onSuccess { articles ->
               _stateFlow.update { state: ArticlesUiState ->
                  state.copy(
                     articles = articles,
                     isLoading = false,
                  )
               }
            }.onFailure {
               _stateFlow.update { state: ArticlesUiState -> state.copy(isLoading = false) }
               _effectDelegate.emit(
                  ArticlesEffect.ShowError(
                     _stringProvider.getString(R.string.error_articles_load)
                  )
               )
            }
         }
      }
   }

   private fun requestRemove(url: String) {
      viewModelScope.launch {
         _effectDelegate.emit(
            ArticlesEffect.ConfirmRemove(
               message = _stringProvider.getString(R.string.confirm_article_delete),
               actionLabel = _stringProvider.getString(R.string.action_confirm),
               url = url,
            )
         )
      }
   }

   private fun remove(url: String) {
      viewModelScope.launch {
         _articleRepository.remove(url)
            .onSuccess {
               _effectDelegate.emit(
                  ArticlesEffect.ShowMessage(
                     _stringProvider.getString(R.string.message_article_deleted)
                  )
               )
            }
            .onFailure {
               _effectDelegate.emit(
                  ArticlesEffect.ShowError(
                     _stringProvider.getString(R.string.error_article_delete)
                  )
               )
            }
      }
   }
}
