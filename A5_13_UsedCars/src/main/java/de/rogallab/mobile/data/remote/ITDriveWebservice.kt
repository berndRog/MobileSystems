package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.TDriveDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ITDriveWebservice {
   @GET("usedcarsapi/v1/tdrives") suspend fun getAll(): List<TDriveDto>
   @GET("usedcarsapi/v1/tdrives/count") suspend fun countAll(): Int
   @GET("usedcarsapi/v1/tdrives/{id}") suspend fun getById(@Path("id") id: String): TDriveDto
   @POST("usedcarsapi/v1/tdrives") suspend fun create(@Body tDrive: TDriveDto): TDriveDto
   @PUT("usedcarsapi/v1/tdrives/{id}")
   suspend fun update(@Path("id") id: String, @Body tDrive: TDriveDto): TDriveDto
   @DELETE("usedcarsapi/v1/tdrives/{id}") suspend fun delete(@Path("id") id: String): Response<Unit>
}
