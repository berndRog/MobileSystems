package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.INewsRepository
import de.rogallab.mobile.domain.entities.Article

class FakeNewsRepository : INewsRepository {
   data class SearchRequest(val searchText: String, val page: Int)

   var searchResult: Result<List<Article>> = Result.success(emptyList())
   val searchRequests = mutableListOf<SearchRequest>()

   override suspend fun search(
      searchText: String,
      page: Int,
   ): Result<List<Article>> {
      // Record the interaction before returning the configured result.
      searchRequests += SearchRequest(searchText, page)
      return searchResult
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake macht Suchparameter und Result kontrollierbar, ohne einen echten
 *   HTTP-Aufruf auszuführen.
 *
 * Lernziele:
 *
 * - ViewModel-Verhalten getrennt von Retrofit testen.
 * - Aufrufe einer Abhängigkeit über aufgezeichnete Parameter verifizieren.
 */
