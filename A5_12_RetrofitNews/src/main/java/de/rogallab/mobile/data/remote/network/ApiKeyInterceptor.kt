package de.rogallab.mobile.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(
   private val _apiKey: String,
) : Interceptor {
   override fun intercept(chain: Interceptor.Chain): Response {
      val originalRequest = chain.request()
      // Development without a configured key still sends the original request.
      if (_apiKey.isBlank()) return chain.proceed(originalRequest)

      // Add the API key centrally instead of repeating it in every endpoint.
      val request = originalRequest.newBuilder()
         .header("X-Api-Key", _apiKey)
         .build()

      return chain.proceed(request)
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der OkHttp-Interceptor ergänzt den API-Schlüssel zentral für alle Requests.
 *   Webservice-Methoden und Repository müssen den Header daher nicht kennen.
 *
 * - Der Schlüssel stammt aus BuildConfig und wird nicht fest im Quellcode
 *   hinterlegt. Ein leerer Entwicklungswert verändert den Request nicht.
 *
 * Lernziele:
 *
 * - Querschnittliche Netzwerkbelange mit einem Interceptor kapseln.
 * - Konfiguration und fachliche Request-Beschreibung voneinander trennen.
 */
