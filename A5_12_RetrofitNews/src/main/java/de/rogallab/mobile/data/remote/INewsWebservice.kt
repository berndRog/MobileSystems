package de.rogallab.mobile.data.remote

import de.rogallab.mobile.data.remote.dtos.NewsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface INewsWebservice {
   // Describes the NewsAPI endpoint without exposing Retrofit to the repository port.
   @GET("v2/everything")
   suspend fun getEverything(
      @Query("q") searchText: String,
      @Query("page") page: Int,
      @Query("pageSize") pageSize: Int,
      @Query("sortBy") sortBy: String = "publishedAt",
   ): NewsResponseDto
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit erzeugt zur Laufzeit eine Implementierung dieses deklarativen
 *   HTTP-Vertrags. Annotationen beschreiben Pfad und Query-Parameter.
 *
 * - Das Interface gehört zur Data-Schicht und liefert deshalb ein Remote-DTO,
 *   das erst im Repository in Domain-Entitäten umgewandelt wird.
 *
 * Lernziele:
 *
 * - Einen REST-Endpunkt mit Retrofit deklarativ beschreiben.
 * - Webservice-Vertrag und Domain-Repository voneinander trennen.
 */
