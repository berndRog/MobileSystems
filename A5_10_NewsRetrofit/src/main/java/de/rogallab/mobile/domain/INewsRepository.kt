package de.rogallab.mobile.domain

import de.rogallab.mobile.data.dtos.News
import kotlinx.coroutines.flow.Flow

interface INewsRepository {
   fun getEverything(searchText: String, page: Int): Flow<Result<News>>
}

