package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.mapping.toDomain
import de.rogallab.mobile.data.mapping.toDto
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.entities.Article
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
            Result.success(articleDtos.map { articleDto -> articleDto.toDomain() })
         }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override suspend fun save(article: Article): Result<Unit> =
      try {
         _articleDao.save(article.toDto())
         Result.success(Unit)
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   override suspend fun remove(url: String): Result<Unit> =
      try {
         _articleDao.remove(url)
         Result.success(Unit)
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }
}
