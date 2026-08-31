package de.rogallab.mobile.ui.articles

import de.rogallab.mobile.domain.entities.Article

sealed interface ArticlesEffect {
   data class ShowMessage(val message: String) : ArticlesEffect
   data class ShowError(val message: String) : ArticlesEffect
   data class ConfirmRemove(
      val message: String,
      val actionLabel: String,
      val url: String,
   ) : ArticlesEffect
   data class NavigateToArticle(val article: Article) : ArticlesEffect
}
