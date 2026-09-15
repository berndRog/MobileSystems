package de.rogallab.mobile.ui.news

import de.rogallab.mobile.domain.entities.Article

sealed interface NewsEffect {
   data class ShowError(val message: String) : NewsEffect
   data class NavigateToArticle(val article: Article) : NewsEffect
}
