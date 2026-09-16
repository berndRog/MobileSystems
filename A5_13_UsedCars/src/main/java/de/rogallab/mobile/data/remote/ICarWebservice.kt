package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.CarDto
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

interface ICarWebservice {
   @GET("usedcarsapi/v1/cars") suspend fun getAll(): List<CarDto>
   @GET("usedcarsapi/v1/cars/count") suspend fun countAll(): Int
   @GET("usedcarsapi/v1/cars/{id}") suspend fun getById(@Path("id") id: String): CarDto
   @GET("usedcarsapi/v1/people/{id}/cars")
   suspend fun getByPersonId(@Path("id") id: String): List<CarDto>
   @POST("usedcarsapi/v1/cars") suspend fun create(@Body car: CarDto): CarDto
   @PUT("usedcarsapi/v1/cars/{id}")
   suspend fun update(@Path("id") id: String, @Body car: CarDto): CarDto
   @Multipart
   @POST("usedcarsapi/v1/cars/{id}/images")
   suspend fun uploadImage(@Path("id") id: String, @Part file: MultipartBody.Part): CarDto
   @DELETE("usedcarsapi/v1/cars/{id}/images/{fileName}")
   suspend fun deleteImage(
      @Path("id") id: String,
      @Path("fileName") fileName: String,
   ): CarDto
   @DELETE("usedcarsapi/v1/cars/{id}") suspend fun delete(@Path("id") id: String): Response<Unit>
}
