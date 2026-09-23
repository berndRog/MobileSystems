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

   // Dispatches events from the article screen.
   fun onIntent(intent: ArticleIntent) {
      when (intent) {
         ArticleIntent.Save -> save()
      }
   }

   private fun save() {
      viewModelScope.launch {
         // Saving is one direct local repository operation.
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

/*
 * Didaktik und Lernziele
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
