package de.rogallab.mobile.ui.features.article

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.data.dtos.Article

@Immutable
data class WebArticleUiState(
   val isNews: Boolean = true,
   val article: Article? =  null,
)