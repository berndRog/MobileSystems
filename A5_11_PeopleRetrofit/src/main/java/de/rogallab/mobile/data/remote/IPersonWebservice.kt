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

   // Request all people. Retrofit converts the JSON array into PersonDto objects.
   @GET("peopleapi/v1/people")
   suspend fun getAll(): List<PersonDto>

   // Request only the number of stored people without transferring the complete list.
   @GET("peopleapi/v1/people/count")
   suspend fun countAll(): Int

   // Insert the supplied id into the {id} placeholder of the request path.
   // A successful response body is converted into one PersonDto.
   @GET("peopleapi/v1/people/{id}")
   suspend fun getById(
      @Path("id") id: String,
   ): PersonDto

   // POST uses multipart/form-data because person fields and an optional image
   // have to be transmitted together in one HTTP request.
   @Multipart
   @POST("peopleapi/v1/people")
   suspend fun create(
      // Text fields are sent as individual multipart form parts.
      @Part("FirstName") firstName: RequestBody,
      @Part("LastName") lastName: RequestBody,
      @Part("Email") email: RequestBody?,
      @Part("Phone") phone: RequestBody?,
      @Part("Id") id: RequestBody,
      // The image part contains file name, media type and binary file content.
      @Part image: MultipartBody.Part?,
   ): PersonDto

   // PUT uses the person id in the URL and sends the changed data as multipart parts.
   @Multipart
   @PUT("peopleapi/v1/people/{id}")
   suspend fun update(
      @Path("id") id: String,
      @Part("FirstName") firstName: RequestBody,
      @Part("LastName") lastName: RequestBody,
      @Part("Email") email: RequestBody?,
      @Part("Phone") phone: RequestBody?,
      // RemoveImage distinguishes keeping an existing image from deleting it.
      @Part("RemoveImage") removeImage: RequestBody,
      // A non-null image replaces the currently stored server image.
      @Part image: MultipartBody.Part?,
   ): PersonDto

   // DELETE has no Person response body. Response<Unit> keeps the HTTP status
   // available so the repository can verify that the request succeeded.
   @DELETE("peopleapi/v1/people/{id}")
   suspend fun delete(
      @Path("id") id: String,
   ): Response<Unit>
}

/*
 * Didaktik und Lernziele
 *
 * - IPersonWebservice beschreibt den HTTP-Vertrag zwischen Android-Client und
 *   PeopleApi. Die Retrofit-Annotationen legen fest, welche HTTP-Methode, welcher
 *   Pfad und welche Parameter für einen Aufruf verwendet werden.
 *
 * - @GET, @POST, @PUT und @DELETE entsprechen unmittelbar den HTTP-Methoden der
 *   REST-API. @Path ersetzt einen Platzhalter wie {id} durch den übergebenen Wert.
 *   Die suspend-Funktionen können dadurch aus Coroutines aufgerufen werden, ohne
 *   den aufrufenden Thread während der Netzwerkoperation zu blockieren.
 *
 * - GET liefert JSON. GsonConverterFactory übersetzt die Antwort automatisch in
 *   List<PersonDto>, PersonDto bzw. Int. Die Retrofit-Schnittstelle arbeitet damit
 *   mit Transportobjekten und nicht mit der Domain-Entität Person.
 *
 * - countAll() ist eine gezielte Abfrage für das Client-Seeding. Statt zuerst alle
 *   Personen zu übertragen und anschließend list.size auszuwerten, liefert die
 *   WebAPI nur die Anzahl der Datensätze.
 *
 * - Create und Update verwenden multipart/form-data. Die einfachen Personendaten
 *   werden als RequestBody-Parts übertragen; ein optionales Bild wird als
 *   MultipartBody.Part mit Dateiname, Content-Type und Binärdaten mitgesendet.
 *   Dadurch gehören Person und Bild zu genau einem fachlichen HTTP-Request.
 *
 * - Beim Update beschreibt RemoveImage zusätzlich die gewünschte Bildoperation:
 *   ohne neues Bild und RemoveImage=false bleibt das Serverbild erhalten,
 *   RemoveImage=true löscht es, ein neues image-Part ersetzt es.
 *
 * - DELETE liefert keinen PersonDto zurück. Response<Unit> wird verwendet, damit
 *   das Repository trotzdem den HTTP-Status auswerten und Fehler erkennen kann.
 *
 * - Die Rolle ähnelt IPersonDao aus A5_01: Beide Schnittstellen kapseln den Zugang
 *   zur Datenquelle. Das DAO beschreibt jedoch lokale Room-Operationen, während
 *   IPersonWebservice den entfernten HTTP-Vertrag der PeopleApi beschreibt.
 */
