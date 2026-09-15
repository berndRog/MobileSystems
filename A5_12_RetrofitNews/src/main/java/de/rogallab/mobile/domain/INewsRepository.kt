package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Article

interface INewsRepository {
   suspend fun search(
      searchText: String,
      page: Int = 1,
   ): Result<List<Article>>
}
