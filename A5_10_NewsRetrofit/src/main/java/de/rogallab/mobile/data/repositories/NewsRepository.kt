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
   ): Result<List<Article>> = try {
      val response = _newsWebservice.getEverything(
         searchText = searchText.trim(),
         page = page,
         pageSize = Globals.pageSize,
      )

      Result.success(
         response.articles.mapNotNull { articleDto ->
            articleDto.toDomainOrNull()
         }
      )
   }
   catch (exception: CancellationException) {
      throw exception
   }
   catch (throwable: Throwable) {
      Result.failure(throwable)
   }
}
