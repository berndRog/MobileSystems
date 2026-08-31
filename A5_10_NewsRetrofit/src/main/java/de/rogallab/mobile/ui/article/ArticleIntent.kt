package de.rogallab.mobile.ui.article

sealed interface ArticleIntent {
   data object Save : ArticleIntent
}
