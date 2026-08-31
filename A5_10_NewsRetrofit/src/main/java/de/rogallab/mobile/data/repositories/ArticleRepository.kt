package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.local.IArticleDao
import de.rogallab.mobile.data.dtos.Article
import de.rogallab.mobile.domain.IArticleRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.domain.utilities.logDebug
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class ArticleRepository(
   private val _articleDao: IArticleDao,
   private val _dispatcher: CoroutineDispatcher,
   private val _exceptionHandler: CoroutineExceptionHandler
) : IArticleRepository {

   override fun selectArticles(): Flow<Result<List<Article>>> = flow {
      try {
         logDebug(tag, "selectArticles")
         _articleDao.select().distinctUntilChanged().collect { articles: List<Article> ->
            emit(Result.success(articles))
         } // ^flow
      }
      catch(e: CancellationException) { throw e }
      catch(t: Throwable) { emit(Result.failure(t)) }
   }.flowOn(_dispatcher )

   override suspend fun upsert(article: Article): Result<Unit> =
      withContext(_dispatcher + _exceptionHandler) {
         return@withContext try {
            logDebug(tag, "upsert article")
            _articleDao.upsert(article)
            Result.success(Unit)
         } catch (t: Throwable) {
            Result.failure(t)
         }
      }

   override suspend fun remove(article: Article): Result<Unit> =
      withContext(_dispatcher + _exceptionHandler) {
         return@withContext try {
            logDebug(tag, "delete article")
            _articleDao.remove(article)
            Result.success(Unit)
         } catch (t: Throwable) {
            Result.failure(t)
         }
      }

   companion object {
      private const val tag = "<-ArticleRepository"
   }
}