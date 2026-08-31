package de.rogallab.mobile.ui.articles

import de.rogallab.mobile.domain.entities.Article

sealed interface ArticlesIntent {
   data class Detail(val article: Article) : ArticlesIntent
   data class RequestRemove(val url: String) : ArticlesIntent
   data class ConfirmRemove(val url: String) : ArticlesIntent
}
