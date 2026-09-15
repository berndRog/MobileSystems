package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
   fun observeAll(): Flow<Result<List<Article>>>
   suspend fun save(article: Article): Result<Unit>
   suspend fun remove(url: String): Result<Unit>
}
