package de.rogallab.mobile.ui.article

sealed interface ArticleEffect {
   data class ShowMessage(val message: String) : ArticleEffect
   data class ShowError(val message: String) : ArticleEffect
}
