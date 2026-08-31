package de.rogallab.mobile.domain

import de.rogallab.mobile.data.dtos.Article
import kotlinx.coroutines.flow.Flow

interface IArticleRepository {
   fun selectArticles(): Flow<Result<List<Article>>>
   suspend fun upsert(article: Article): Result<Unit>
   suspend fun remove(article: Article): Result<Unit>
}

