package de.rogallab.mobile.shared.data.network

import okhttp3.Interceptor
import okhttp3.Response

class NetworkConnectionInterceptor(
   private val _networkConnectionChecker: NetworkConnectionChecker,
) : Interceptor {

   override fun intercept(chain: Interceptor.Chain): Response {
      _networkConnectionChecker.checkConnection()
      return chain.proceed(chain.request())
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Interceptor wird vor jedem OkHttp-Request ausgeführt.
 * - Dadurch wird der Netzwerkzustand zum Zeitpunkt des tatsächlichen Requests
 *   geprüft und nicht nur einmal beim Start der Anwendung.
 * - Schlägt checkConnection() fehl, wird der Request nicht ausgeführt und die
 *   entsprechende IOException bis zum Repository weitergereicht.
 */
