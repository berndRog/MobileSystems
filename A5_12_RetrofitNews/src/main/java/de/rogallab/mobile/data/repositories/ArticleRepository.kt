package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.mapping.toDomain
import de.rogallab.mobile.data.mapping.toDto
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ArticleRepository(
   private val _articleDao: IArticleDao,
) : IArticleRepository {

   override fun observeAll(): Flow<Result<List<Article>>> =
      _articleDao.observeAll()
         .map { articleDtos ->
            // Map Room DTOs before exposing them to the domain and UI layers.
            Result.success(articleDtos.map { articleDto -> articleDto.toDomain() })
         }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override suspend fun save(article: Article): Result<Unit> =
      resultOf(
         block = {
            // Room receives only its own persistence DTO.
            val rowId = _articleDao.save(article.toDto())
            checkInserted(rowId)
         }
      )

   override suspend fun remove(url: String): Result<Unit> =
      resultOf(
         block = {
            // The article URL is the primary key used by the DAO.
            val rowsAffected = _articleDao.remove(url)
            checkDeleted(rowsAffected)
         }
      )
}

/*
 * Didaktik und Lernziele
 *
 * - ArticleRepository kapselt DAO und Mapping für die lokale Artikelsammlung.
 *   ViewModels arbeiten ausschließlich mit der Domain-Entität Article.
 *
 * - observeAll() bewahrt die Reaktivität von Room. save() und remove() sind
 *   dagegen einzelne suspendierende Schreiboperationen mit Result-Rückgabe.
 *
 * - Auch hier bleibt Coroutine-Cancellation erhalten und wird nicht in einen
 *   fachlich scheinenden Fehlerwert umgewandelt.
 *
 * Lernziele:
 *
 * - DTOs und Domain-Entitäten über ein Repository entkoppeln.
 * - Flow-basierte Beobachtung mit suspendierenden Schreibzugriffen kombinieren.
 */
