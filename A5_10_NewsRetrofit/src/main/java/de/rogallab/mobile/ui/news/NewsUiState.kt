package de.rogallab.mobile.ui.news

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Article

@Immutable
data class NewsUiState(
   val searchText: String = "",
   val articles: List<Article> = emptyList(),
   val isLoading: Boolean = false,
)
