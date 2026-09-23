package de.rogallab.mobile.testing

import de.rogallab.mobile.data.remote.INewsWebservice
import de.rogallab.mobile.data.remote.dtos.NewsResponseDto

class FakeNewsWebservice : INewsWebservice {
   data class Request(
      val searchText: String,
      val page: Int,
      val pageSize: Int,
      val sortBy: String,
   )

   var response = NewsResponseDto()
   var failure: Throwable? = null
   val requests = mutableListOf<Request>()

   override suspend fun getEverything(
      searchText: String,
      page: Int,
      pageSize: Int,
      sortBy: String,
   ): NewsResponseDto {
      failure?.let { throwable -> throw throwable }
      requests += Request(searchText, page, pageSize, sortBy)
      return response
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake ersetzt den von Retrofit erzeugten Webservice und zeichnet den
 *   vollständigen HTTP-Auftrag auf, ohne Netzwerkzugriff zu benötigen.
 *
 * Lernziele:
 *
 * - Repository-Mapping und Request-Parameter isoliert testen.
 */
