package de.rogallab.mobile.data.remote.dtos

data class NewsResponseDto(
   val status: String = "",
   val totalResults: Int = 0,
   val articles: List<ArticleRemoteDto> = emptyList(),
)

data class ArticleRemoteDto(
   val source: SourceDto? = null,
   val author: String? = null,
   val title: String? = null,
   val description: String? = null,
   val url: String? = null,
   val urlToImage: String? = null,
   val publishedAt: String? = null,
   val content: String? = null,
)

data class SourceDto(
   val id: String? = null,
   val name: String? = null,
)
