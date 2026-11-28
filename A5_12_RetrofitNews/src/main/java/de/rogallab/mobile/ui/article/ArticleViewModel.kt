package de.rogallab.mobile.ui.article

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.launch

class ArticleViewModel(
   val article: Article,
   private val _articleRepository: IArticleRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<ArticleEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<ArticleEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<ArticleEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { ArticleEffect.ShowError(it) },
      _messageEffect = { ArticleEffect.ShowMessage(it) },
      _tag = "<-ArticleViewModel",
   )

   // Dispatches events from the article screen.
   fun onIntent(intent: ArticleIntent) {
      when (intent) {
         ArticleIntent.Save -> save()
      }
   }

   private fun save() {
      viewModelScope.launch {
         // Saving is one direct local repository operation.
         delay(_timing.delayWrite)

         _articleRepository.save(article)
            .onSuccess {
               _effects.showMessage(R.string.message_article_saved)
            }
            .onFailure { throwable ->
               _effects.showError(R.string.error_article_save, throwable)
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
 * - ArticleViewModel besitzt keinen eigenen veränderlichen UI-State, weil der
 *   ausgewählte Artikel vollständig als Navigationseintrag übergeben wird.
 *
 * - Save delegiert genau einen lokalen Schreibzugriff an IArticleRepository.
 *   Ein eigener Use Case würde derzeit keine zusätzliche fachliche Regel kapseln.
 *
 * - Erfolgs- und Fehlermeldungen sind einmalige Effects und werden nicht als
 *   dauerhafter Zustand im ViewModel gespeichert.
 *
 * Lernziele:
 *
 * - Auch bei Schreibzugriffen zwischen einfacher Delegation und Orchestrierung
 *   mehrerer Schritte unterscheiden.
 * - Rückmeldungen einer Operation über Effects ausgeben.
 */
