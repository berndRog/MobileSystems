package de.rogallab.mobile.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
   private val _newsRepository: INewsRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<NewsEffect>,
) : ViewModel(), IEffectSource<NewsEffect> by _effectDelegate {

   // Mutable UI state remains private to the ViewModel.
   private val _stateFlow = MutableStateFlow(NewsUiState())
   // The screen observes only this read-only state representation.
   val stateFlow: StateFlow<NewsUiState> = _stateFlow.asStateFlow()

   // Dispatches all UI events through one public entry point.
   fun onIntent(intent: NewsIntent) {
      when (intent) {
         is NewsIntent.SearchTextChanged -> {
            _stateFlow.update { state: NewsUiState ->
               state.copy(searchText = intent.value)
            }
         }
         NewsIntent.Search -> search()
         is NewsIntent.Detail -> navigateToArticle(intent.article)
      }
   }

   private fun search() {
      // Normalize and validate the user input before starting network work.
      val searchText = _stateFlow.value.searchText.trim()
      if (searchText.isBlank()) {
         showError(_stringProvider.getString(R.string.error_search_required))
         return
      }

      _stateFlow.update { state: NewsUiState ->
         state.copy(isLoading = true)
      }
      viewModelScope.launch {
         // This simple read operation delegates directly to one repository.
         _newsRepository.search(searchText)
            .onSuccess { articles ->
               _stateFlow.update { state: NewsUiState ->
                  state.copy(articles = articles, isLoading = false)
               }
            }
            .onFailure {
               _stateFlow.update { state: NewsUiState -> state.copy(isLoading = false) }
               _effectDelegate.emit(
                  NewsEffect.ShowError(
                     _stringProvider.getString(R.string.error_news_load)
                  )
               )
            }
      }
   }

   private fun navigateToArticle(article: de.rogallab.mobile.domain.entities.Article) {
      viewModelScope.launch {
         _effectDelegate.emit(NewsEffect.NavigateToArticle(article))
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(NewsEffect.ShowError(message))
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - NewsViewModel verarbeitet Suchtext, Ladezustand und Suchergebnisse als
 *   persistenten UI-State. Fehler und Navigation bleiben einmalige Effects.
 *
 * - Für search(...) wird bewusst kein eigener Use Case eingeführt. Nach der
 *   einfachen Eingabeprüfung delegiert das ViewModel genau einen Lesezugriff
 *   an INewsRepository; zusätzliche fachliche Koordination existiert nicht.
 *
 * - Ein pass-through NewsUcSearch würde nur eine weitere Klasse und DI-
 *   Abhängigkeit ergänzen, ohne Verhalten zu kapseln. Ein Use Case wäre sinnvoll,
 *   sobald beispielsweise mehrere Quellen, Cache-Regeln oder Paging-Strategien
 *   zu einer Anwendungsoperation kombiniert werden.
 *
 * Lernziele:
 *
 * - State und einmalige Effects im UDF-Datenfluss unterscheiden.
 * - Use Cases aufgrund vorhandener Anwendungslogik statt pauschal einsetzen.
 * - Einfache Read-Zugriffe direkt über einen stabilen Repository-Port ausführen.
 */
