package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface INewsWebservice {
   @GET("v2/everything")
   suspend fun getEverything(
      @Query("q") searchText: String,
      @Query("page") page: Int,
      @Query("pageSize") pageSize: Int,
      @Query("sortBy") sortBy: String = "publishedAt",
   ): NewsResponseDto
}
