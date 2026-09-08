package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.PersonDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface IPersonWebservice {

   @GET("peopleapi/v1/people")
   suspend fun getAll(): List<PersonDto>

   @GET("peopleapi/v1/people/{id}")
   suspend fun getById(
      @Path("id") id: String,
   ): PersonDto

   @Multipart
   @POST("peopleapi/v1/people")
   suspend fun create(
      @Part("FirstName") firstName: RequestBody,
      @Part("LastName") lastName: RequestBody,
      @Part("Email") email: RequestBody?,
      @Part("Phone") phone: RequestBody?,
      @Part("Id") id: RequestBody,
      @Part image: MultipartBody.Part?,
   ): PersonDto

   @Multipart
   @PUT("peopleapi/v1/people/{id}")
   suspend fun update(
      @Path("id") id: String,
      @Part("FirstName") firstName: RequestBody,
      @Part("LastName") lastName: RequestBody,
      @Part("Email") email: RequestBody?,
      @Part("Phone") phone: RequestBody?,
      @Part("RemoveImage") removeImage: RequestBody,
      @Part image: MultipartBody.Part?,
   ): PersonDto

   @DELETE("peopleapi/v1/people/{id}")
   suspend fun delete(
      @Path("id") id: String,
   ): Response<Unit>
}

/*
 * Didaktik und Lernziele
 *
 * - IPersonWebservice beschreibt ausschließlich den HTTP-Vertrag der PeopleApi.
 * - GET liefert JSON und wird durch Gson direkt in PersonDto übersetzt.
 * - POST und PUT verwenden multipart/form-data, weil Personendaten und optional
 *   eine Bilddatei gemeinsam in genau einem Request übertragen werden.
 * - Das Webservice kennt weder Domain-Entität noch ViewModel. Diese Entkopplung
 *   entspricht der Rolle des DAO in A5_01, nur mit HTTP statt SQLite.
 */
