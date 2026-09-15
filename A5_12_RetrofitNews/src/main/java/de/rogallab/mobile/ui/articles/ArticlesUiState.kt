package de.rogallab.mobile.ui.articles

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Article

@Immutable
data class ArticlesUiState(
   val articles: List<Article> = emptyList(),
   val isLoading: Boolean = true,
)
