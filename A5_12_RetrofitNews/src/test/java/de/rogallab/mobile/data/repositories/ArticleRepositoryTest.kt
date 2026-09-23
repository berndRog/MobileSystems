package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.dtos.ArticleDto
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.testing.FakeArticleDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleRepositoryTest {

   private val article = Article(
      url = "https://example.test/article",
      sourceName = "Example News",
      author = "Ada",
      title = "Kotlin News",
      publishedAt = "2026-09-23T10:00:00Z",
   )

   @Test
   fun observeAll_mapsDtosToDomainArticles() = runTest {
      val dao = FakeArticleDao(
         listOf(
            ArticleDto(
               url = article.url,
               sourceName = article.sourceName,
               author = article.author,
               title = article.title,
               description = null,
               content = null,
               publishedAt = article.publishedAt,
               imageUrl = null,
            )
         )
      )

      val result = ArticleRepository(dao).observeAll().first()

      assertEquals(listOf(article), result.getOrThrow())
   }

   @Test
   fun observeAll_daoFailure_emitsFailure() = runTest {
      val failure = IllegalStateException("observe failed")
      val dao = FakeArticleDao().apply { observeFailure = failure }

      val result = ArticleRepository(dao).observeAll().first()

      assertTrue(result.isFailure)
      assertEquals(failure, result.exceptionOrNull())
   }

   @Test
   fun save_mapsArticleAndDelegatesToDao() = runTest {
      val dao = FakeArticleDao()

      val result = ArticleRepository(dao).save(article)

      assertTrue(result.isSuccess)
      assertEquals(article.url, dao.saved.single().url)
      assertEquals(article.title, dao.saved.single().title)
   }

   @Test
   fun save_daoFailure_returnsFailure() = runTest {
      val failure = IllegalStateException("save failed")
      val dao = FakeArticleDao().apply { saveFailure = failure }

      val result = ArticleRepository(dao).save(article)

      assertTrue(result.isFailure)
      assertEquals(failure, result.exceptionOrNull())
   }

   @Test
   fun remove_delegatesUrlToDao() = runTest {
      val dao = FakeArticleDao()

      val result = ArticleRepository(dao).remove(article.url)

      assertTrue(result.isSuccess)
      assertEquals(listOf(article.url), dao.removedUrls)
   }

   @Test
   fun remove_daoFailure_returnsFailure() = runTest {
      val failure = IllegalStateException("remove failed")
      val dao = FakeArticleDao().apply { removeFailure = failure }

      val result = ArticleRepository(dao).remove(article.url)

      assertTrue(result.isFailure)
      assertEquals(failure, result.exceptionOrNull())
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen Mapping und DAO-Delegation des lokalen Repositorys ohne
 *   eine echte Room-Datenbank zu starten.
 *
 * Lernziele:
 *
 * - Flow-basierte Lesezugriffe und suspendierende Schreibzugriffe isoliert testen.
 * - Data-Schicht und Datenbanktechnik über ein DAO-Test-Double entkoppeln.
 */
