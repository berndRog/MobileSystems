package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.dtos.News
import de.rogallab.mobile.data.remote.INewsWebservice
import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.ResultData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NewsRepository(
   private val _newsWebservice: INewsWebservice,
   private val _dispatcher: CoroutineDispatcher
) : INewsRepository {

   override fun getEverything(searchText: String, page: Int): Flow<Result<News>> = flow {
      if (searchText.isEmpty()) {
         emit(Result.success(News()))
      } else {
         try {
            // make the api call
            val news: News = _newsWebservice.getEverything(searchText, page)
            emit(Result.success(news))
         }
         catch(e: CancellationException ) { throw e }
         catch(t: Throwable) { emit(Result.failure(t)) }
      }
   }.flowOn(_dispatcher)

   companion object {
      private const val tag = "<-NewsRepository"
   }
}