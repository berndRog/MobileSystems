package de.rogallab.mobile.data.remote


import de.rogallab.mobile.data.dtos.News
import retrofit2.http.GET
import retrofit2.http.Query

interface INewsWebservice {

   // BaseUrl https://newsapi.org/
   @GET("v2/everything")
   suspend fun getEverything(
      @Query("q")
      text: String,
      @Query("page")
      page: Int = 1,
      @Query("pagesize")
      pageSize: Int = 100
   ): News
}