package de.rogallab.mobile.testing

import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.local.dtos.ArticleDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class FakeArticleDao(
   articles: List<ArticleDto> = emptyList(),
) : IArticleDao {
   val articlesFlow = MutableStateFlow(articles)

   var observeFailure: Throwable? = null
   var saveFailure: Throwable? = null
   var removeFailure: Throwable? = null

   val saved = mutableListOf<ArticleDto>()
   val removedUrls = mutableListOf<String>()

   override fun observeAll(): Flow<List<ArticleDto>> = flow {
      observeFailure?.let { throwable -> throw throwable }
      emitAll(articlesFlow)
   }

   override suspend fun save(articleDto: ArticleDto) {
      saveFailure?.let { throwable -> throw throwable }
      saved += articleDto
      articlesFlow.value =
         articlesFlow.value.filterNot { it.url == articleDto.url } + articleDto
   }

   override suspend fun remove(url: String): Int {
      removeFailure?.let { throwable -> throw throwable }
      removedUrls += url
      val previousSize = articlesFlow.value.size
      articlesFlow.value = articlesFlow.value.filterNot { it.url == url }
      return previousSize - articlesFlow.value.size
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake bildet die für das Repository relevanten DAO-Eigenschaften ab:
 *   beobachtbare Daten, Ersetzen beim Speichern und Löschen über die URL.
 *
 * Lernziele:
 *
 * - Repository-Tests ohne Room-Datenbank ausführen.
 * - DAO-Seiteneffekte und Fehler kontrollierbar machen.
 */
