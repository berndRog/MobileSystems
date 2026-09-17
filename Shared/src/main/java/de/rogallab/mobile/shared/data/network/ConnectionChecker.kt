package de.rogallab.mobile.shared.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectionChecker(
   context: Context,
) {

   private val _connectivityManager: ConnectivityManager =
      context.getSystemService(ConnectivityManager::class.java)

   fun checkConnection() {
      val network = _connectivityManager.activeNetwork
         ?: throw NoNetworkConnectionException()

      val capabilities =
         _connectivityManager.getNetworkCapabilities(network)
            ?: throw NoNetworkConnectionException()

      val hasNetworkTransport =
         capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
         capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
         capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
         capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

      if (!hasNetworkTransport)
         throw NoNetworkConnectionException()

      val hasInternetCapability =
         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

      val isValidated =
         capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

      if (!hasInternetCapability || !isValidated)
         throw NoInternetConnectionException()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ConnectivityManager beschreibt den aktuell von Android verwendeten Netzwerkpfad.
 * - NetworkCapabilities unterscheidet Transportarten wie WLAN, Mobilfunk, Ethernet
 *   oder VPN und beschreibt zusätzlich die Fähigkeiten des aktiven Netzwerks.
 * - NET_CAPABILITY_INTERNET bedeutet, dass das Netzwerk grundsätzlich für
 *   Internetkommunikation vorgesehen ist.
 * - NET_CAPABILITY_VALIDATED bedeutet, dass Android den Internetzugang tatsächlich
 *   erfolgreich geprüft hat. Ein verbundenes WLAN allein garantiert keinen
 *   funktionierenden Internetzugang.
 * - Die beiden Exceptions unterscheiden zwischen fehlender Netzwerkverbindung und
 *   vorhandener Verbindung ohne bestätigten Internetzugang.
 */
