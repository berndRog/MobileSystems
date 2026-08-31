package de.rogallab.mobile.ui.features.news

import androidx.compose.runtime.Immutable

@Immutable
data class SearchUiState(
   val searchText: String = "",
)