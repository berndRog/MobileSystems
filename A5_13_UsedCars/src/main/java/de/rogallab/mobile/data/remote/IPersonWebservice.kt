package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.PersonDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface IPersonWebservice {
   @GET("usedcarsapi/v1/people") suspend fun getAll(): List<PersonDto>
   @GET("usedcarsapi/v1/people/count") suspend fun countAll(): Int
   @GET("usedcarsapi/v1/people/{id}") suspend fun getById(@Path("id") id: String): PersonDto
   @POST("usedcarsapi/v1/people") suspend fun create(@Body person: PersonDto): PersonDto
   @PUT("usedcarsapi/v1/people/{id}")
   suspend fun update(@Path("id") id: String, @Body person: PersonDto): PersonDto
   @Multipart
   @POST("usedcarsapi/v1/people/{id}/image")
   suspend fun uploadImage(@Path("id") id: String, @Part file: MultipartBody.Part): PersonDto
   @DELETE("usedcarsapi/v1/people/{id}/image")
   suspend fun deleteImage(@Path("id") id: String): PersonDto
   @DELETE("usedcarsapi/v1/people/{id}") suspend fun delete(@Path("id") id: String): Response<Unit>
}
