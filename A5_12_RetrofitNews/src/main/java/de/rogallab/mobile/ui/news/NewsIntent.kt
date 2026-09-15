package de.rogallab.mobile.ui.news

import de.rogallab.mobile.domain.entities.Article

sealed interface NewsIntent {
   data class SearchTextChanged(val value: String) : NewsIntent
   data object Search : NewsIntent
   data class Detail(val article: Article) : NewsIntent
}
