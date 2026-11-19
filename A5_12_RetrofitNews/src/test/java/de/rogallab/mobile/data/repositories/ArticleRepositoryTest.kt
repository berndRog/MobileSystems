package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IArticleDao
import kotlinx.coroutines.flow.flow
import de.rogallab.mobile.data.local.dtos.ArticleDto
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.testing.FakeArticleDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import kotlin.coroutines.cancellation.CancellationException
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

      assertEquals(Unit, result.getOrThrow())
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

      val repository = ArticleRepository(dao)
      repository.save(article).getOrThrow()
      val result = repository.remove(article.url)

      assertEquals(Unit, result.getOrThrow())
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

   @Test
   fun saveAndRemove_rethrowCancellation() = runTest {
      val cancellation = CancellationException("write cancelled")
      val dao = FakeArticleDao().apply {
         saveFailure = cancellation
         removeFailure = cancellation
      }
      val repository = ArticleRepository(dao)
      val operations: List<suspend () -> Result<Unit>> = listOf(
         { repository.save(article) },
         { repository.remove(article.url) },
      )
      for (operation in operations) {
         try {
            operation()
            fail("CancellationException must propagate")
         } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
         }
      }
   }

   @Test
   fun save_invalidRowId_returnsFailure() = runTest {
      val dao = object : IArticleDao by FakeArticleDao() {
         override suspend fun save(articleDto: ArticleDto): Long = -1L
      }
      assertTrue(ArticleRepository(dao).save(article).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun remove_unexpectedRowCounts_returnFailures() = runTest {
      for (rows in listOf(-1, 0, 2)) {
         val dao = object : IArticleDao by FakeArticleDao() {
            override suspend fun remove(url: String): Int = rows
         }
         assertTrue(ArticleRepository(dao).remove(article.url).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun save_replacesExistingArticleSuccessfully() = runTest {
      val dao = FakeArticleDao()
      val repository = ArticleRepository(dao)
      repository.save(article).getOrThrow()
      repository.save(article.copy(title = "Updated")).getOrThrow()
      assertEquals(listOf(article.copy(title = "Updated")), repository.observeAll().first().getOrThrow())
   }

   @Test
   fun observeAll_rethrowsCancellation() = runTest {
      val cancellation = CancellationException("observe cancelled")
      val dao = object : IArticleDao by FakeArticleDao() {
         override fun observeAll() = flow<List<ArticleDto>> { throw cancellation }
      }
      try {
         ArticleRepository(dao).observeAll().first()
         fail("CancellationException must propagate")
      } catch (actual: CancellationException) {
         assertSame(cancellation, actual)
      }
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
