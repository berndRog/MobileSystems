package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeArticleRepository(
   articles: List<Article> = emptyList(),
) : IArticleRepository {
   val articlesFlow = MutableStateFlow(Result.success(articles))

   var saveResult: Result<Unit> = Result.success(Unit)
   var removeResult: Result<Unit> = Result.success(Unit)

   val saved = mutableListOf<Article>()
   val removedUrls = mutableListOf<String>()

   override fun observeAll(): Flow<Result<List<Article>>> = articlesFlow

   override suspend fun save(article: Article): Result<Unit> {
      if (saveResult.isSuccess) {
         saved += article
         val articles = articlesFlow.value.getOrDefault(emptyList())
         articlesFlow.value = Result.success(
            articles.filterNot { it.url == article.url } + article
         )
      }
      return saveResult
   }

   override suspend fun remove(url: String): Result<Unit> {
      if (removeResult.isSuccess) {
         removedUrls += url
         val articles = articlesFlow.value.getOrDefault(emptyList())
         articlesFlow.value = Result.success(
            articles.filterNot { it.url == url }
         )
      }
      return removeResult
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake bildet das beobachtbare Verhalten des Room-Repositorys im Speicher
 *   nach. Erfolgreiche Schreibzugriffe erzeugen einen neuen Flow-Zustand.
 *
 * Lernziele:
 *
 * - Reaktive Repository-Abhängigkeiten ohne Datenbank testen.
 * - Erfolgs- und Fehlerpfade gezielt konfigurieren.
 */
