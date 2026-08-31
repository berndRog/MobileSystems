package de.rogallab.mobile.ui.features.article

import de.rogallab.mobile.data.dtos.Article

sealed class ArticleIntent {
   data class  ShowWebArticle(val isNews: Boolean, val article: Article) : ArticleIntent()
   data object SaveArticle: ArticleIntent()
   data class RemoveArticle(val article: Article) : ArticleIntent()
   data object UndoRemoveArticle : ArticleIntent()
}