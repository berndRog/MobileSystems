package de.rogallab.mobile.ui.features.images

sealed class ImageIntent {
   data object FetchDogs: ImageIntent()
   data class SearchDog(val query: String) : ImageIntent()
   data class QueryChange(val query: String) : ImageIntent()
   data class ActiveChange(val active: Boolean) : ImageIntent()
}