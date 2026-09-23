package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.remote.dtos.ArticleRemoteDto
import de.rogallab.mobile.data.remote.dtos.NewsResponseDto
import de.rogallab.mobile.data.remote.dtos.SourceDto
import de.rogallab.mobile.testing.FakeNewsWebservice
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRepositoryTest {

   @Test
   fun search_normalizesRequestAndMapsValidArticles() = runTest {
      val webservice = FakeNewsWebservice().apply {
         response = NewsResponseDto(
            articles = listOf(
               ArticleRemoteDto(
                  source = SourceDto(name = " Example News "),
                  author = " Ada ",
                  title = " Kotlin News ",
                  description = " Description ",
                  url = " https://example.test/kotlin ",
                  urlToImage = " https://example.test/image.jpg ",
                  publishedAt = " 2026-09-23T10:00:00Z ",
                  content = " Content ",
               ),
               ArticleRemoteDto(title = "Missing URL"),
            )
         )
      }
      val repository = NewsRepository(webservice)

      val result = repository.search("  kotlin  ", page = 2)

      assertTrue(result.isSuccess)
      assertEquals(
         FakeNewsWebservice.Request(
            searchText = "kotlin",
            page = 2,
            pageSize = Globals.pageSize,
            sortBy = "publishedAt",
         ),
         webservice.requests.single(),
      )
      val article = result.getOrThrow().single()
      assertEquals("https://example.test/kotlin", article.url)
      assertEquals("Example News", article.sourceName)
      assertEquals("Ada", article.author)
      assertEquals("Kotlin News", article.title)
      assertEquals("https://example.test/image.jpg", article.imageUrl)
   }

   @Test
   fun search_webserviceFailure_returnsFailure() = runTest {
      val failure = IllegalStateException("network failed")
      val repository = NewsRepository(
         FakeNewsWebservice().apply { this.failure = failure }
      )

      val result = repository.search("kotlin")

      assertTrue(result.isFailure)
      assertEquals(failure, result.exceptionOrNull())
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Tests prüfen Request-Normalisierung, Paging-Parameter, Mapping und das
 *   Filtern ungültiger API-Datensätze ohne echten Netzwerkzugriff.
 *
 * Lernziele:
 *
 * - Eine Repository-Grenze mit einem Webservice-Fake isoliert testen.
 * - Erfolgs- und Fehlerpfad eines entfernten Lesezugriffs absichern.
 */
