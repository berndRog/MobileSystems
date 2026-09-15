package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.ArticleDto
import de.rogallab.mobile.data.remote.dtos.ArticleRemoteDto
import de.rogallab.mobile.domain.entities.Article

fun ArticleDto.toDomain(): Article = Article(
   url = url,
   sourceName = sourceName,
   author = author,
   title = title,
   description = description,
   content = content,
   publishedAt = publishedAt,
   imageUrl = imageUrl,
)

fun Article.toDto(): ArticleDto = ArticleDto(
   url = url,
   sourceName = sourceName,
   author = author,
   title = title,
   description = description,
   content = content,
   publishedAt = publishedAt,
   imageUrl = imageUrl,
)

fun ArticleRemoteDto.toDomainOrNull(): Article? {
   val articleUrl = url?.trim().orEmpty()
   if (articleUrl.isBlank()) return null

   return Article(
      url = articleUrl,
      sourceName = source?.name?.trim().orEmpty(),
      author = author?.trim()?.takeUnless(String::isBlank),
      title = title?.trim().orEmpty(),
      description = description?.trim()?.takeUnless(String::isBlank),
      content = content?.trim()?.takeUnless(String::isBlank),
      publishedAt = publishedAt?.trim().orEmpty(),
      imageUrl = urlToImage?.trim()?.takeUnless(String::isBlank),
   )
}
