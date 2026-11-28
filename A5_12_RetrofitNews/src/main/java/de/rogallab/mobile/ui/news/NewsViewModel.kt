package de.rogallab.mobile.ui.news

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewsViewModel(
   private val _newsRepository: INewsRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<NewsEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<NewsEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<NewsEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { NewsEffect.ShowError(it) },
      _tag = "<-NewsViewModel",
   )

   // Mutable UI state remains private to the ViewModel.
   private val _stateFlow = MutableStateFlow(NewsUiState())
   // The screen observes only this read-only state representation.
   val stateFlow: StateFlow<NewsUiState> = _stateFlow.asStateFlow()

   // Dispatches all UI events through one public entry point.
   fun onIntent(intent: NewsIntent) {
      when (intent) {
         is NewsIntent.SearchTextChanged -> changeSearchText(intent.value)
         NewsIntent.Search -> search()
         is NewsIntent.Detail -> _effects.launchEmit(NewsEffect.NavigateToArticle(intent.article))
      }
   }

   private fun changeSearchText(searchText: String) =
      _stateFlow.update { state ->
         state.copy(searchText = searchText)
      }

   private fun search() {
      // Normalize and validate the user input before starting network work.
      val searchText = _stateFlow.value.searchText.trim()
      if (searchText.isBlank()) {
         _effects.launchShowError(R.string.error_search_required)
         return
      }

      _stateFlow.update { state: NewsUiState ->
         state.copy(isLoading = true)
      }
      viewModelScope.launch {
         // This simple read operation delegates directly to one repository.
         delay(_timing.delayRead)

         _newsRepository.search(searchText)
            .onSuccess { articles ->
               _stateFlow.update { state: NewsUiState ->
                  state.copy(articles = articles, isLoading = false)
               }
            }
            .onFailure { throwable ->
               _stateFlow.update { state: NewsUiState -> state.copy(isLoading = false) }
               _effects.showError(R.string.error_news_load, throwable)
            }
      }
   }



}

/*
 * Didaktik und Lernziele
 *
 * - EffectDispatcher bereitet Meldungen auf und sendet über denselben Delegate.
 *   Persistente Fehler und fachliche Entscheidungen bleiben im ViewModel.
 * - launch-Aufrufe starten Versand aus Intents; showError, showMessage und emit
 *   bleiben im Kontext laufender Operationen und erhalten deren Reihenfolge.
 * - Result-Callbacks, try/catch/finally, Guards und technische Cleanup-Abläufe
 *   bleiben erhalten. Ein Fehler wird am Aufrufer nicht zusätzlich protokolliert.
 *
 * Lernziele:
 * - Meldungsaufbereitung von fachlicher Fehlerbehandlung unterscheiden.
 * - Coroutine-Lebensdauer und sequenziellen Versand typisierter Effects erhalten.
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
