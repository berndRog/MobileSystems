package de.rogallab.mobile.ui.features.news

sealed class NewsIntent {
   data class  SearchTextChange(val searchText: String) : NewsIntent()
   data object TriggerSearch : NewsIntent()
}