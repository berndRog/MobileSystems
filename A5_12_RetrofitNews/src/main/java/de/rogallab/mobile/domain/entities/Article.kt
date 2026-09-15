package de.rogallab.mobile.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class Article(
   val url: String,
   val sourceName: String = "",
   val author: String? = null,
   val title: String = "",
   val description: String? = null,
   val content: String? = null,
   val publishedAt: String = "",
   val imageUrl: String? = null,
)
