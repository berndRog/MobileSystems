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

   // Holds the current snapshot of articles stored in Room.
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
         }
         is ArticlesIntent.RequestRemove -> requestRemove(intent.url)
         is ArticlesIntent.ConfirmRemove -> remove(intent.url)
      }
   }

   private fun observeArticles() {
      viewModelScope.launch {
         // Room emits a new list after every successful save or remove operation.
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
         // A confirmed delete is one direct repository operation.
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

/*
 * Didaktik und Lernziele
 *
 * - ArticlesViewModel beobachtet die lokal gespeicherten Artikel reaktiv. Nach
 *   Änderungen liefert Room über das Repository automatisch einen neuen Zustand.
 *
 * - Die Löschbestätigung ist ein UI-Effect; erst ConfirmRemove führt den
 *   Repository-Zugriff aus. Erfolg und Fehler werden ebenfalls als Effects gemeldet.
 *
 * - Ein ArticleUcRemove würde aktuell nur IArticleRepository.remove(...) ohne
 *   zusätzliche Regel weiterreichen. Deshalb bleibt der direkte Zugriff bewusst
 *   bestehen. Bei Undo, Synchronisation oder zusätzlicher Dateibereinigung wäre
 *   ein Use Case dagegen fachlich begründet.
 *
 * Lernziele:
 *
 * - Einen Room-Flow in beobachtbaren UI-State überführen.
 * - Bestätigung, Zustandsänderung und einmalige Rückmeldung trennen.
 * - Den Einsatz eines Use Cases an zusätzlicher Anwendungslogik festmachen.
 */
