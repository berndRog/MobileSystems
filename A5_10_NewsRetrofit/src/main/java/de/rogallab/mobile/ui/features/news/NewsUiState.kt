package de.rogallab.mobile.ui.features.news

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.data.dtos.News

@Immutable
data class NewsUiState(
   val loading: Boolean = false,
   val news: News? = null,
)