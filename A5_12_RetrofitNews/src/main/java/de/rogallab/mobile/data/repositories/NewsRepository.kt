package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.mapping.toDomainOrNull
import de.rogallab.mobile.data.remote.INewsWebservice
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.entities.Article
import kotlinx.coroutines.CancellationException

class NewsRepository(
   private val _newsWebservice: INewsWebservice,
) : INewsRepository {

   override suspend fun search(
      searchText: String,
      page: Int,
   ): Result<List<Article>> =
      try {
         // The repository owns request normalization and the configured page size.
         val response = _newsWebservice.getEverything(
            searchText = searchText.trim(),
            page = page,
            pageSize = Globals.pageSize,
         )

         Result.success(
            // Invalid remote records are ignored at the mapping boundary.
            response.articles.mapNotNull { articleDto ->
               articleDto.toDomainOrNull()
            }
         )
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }
}

/*
 * Didaktik und Lernziele
 *
 * - NewsRepository übersetzt den anwendungsinternen Suchauftrag in einen
 *   Retrofit-Aufruf und bildet anschließend Remote-DTOs auf Domain-Artikel ab.
 *
 * - Unvollständige API-Datensätze ohne URL werden an der Mapping-Grenze
 *   verworfen. Die UI erhält dadurch nur verwendbare Domain-Objekte.
 *
 * - CancellationException wird erneut geworfen, damit strukturierte Coroutine-
 *   Cancellation nicht fälschlich als normaler Ladefehler behandelt wird.
 *
 * Lernziele:
 *
 * - Request-Normalisierung und Mapping in der Data-Schicht bündeln.
 * - Fehler als Result liefern, Cancellation jedoch unverändert weitergeben.
 */
