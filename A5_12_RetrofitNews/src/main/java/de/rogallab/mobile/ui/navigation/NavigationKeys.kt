package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.entities.Article
import kotlinx.serialization.Serializable

@Serializable
data object NewsKey : NavKey

@Serializable
data object ArticlesKey : NavKey

@Serializable
data class ArticleKey(
   val article: Article,
   val allowSave: Boolean,
) : NavKey
