package de.rogallab.mobile.data.remote.network

import java.net.URI

object ServerUrl {
   private const val AVD_HOST = "10.0.2.2"

   // URLs created through localhost are not reachable from inside the emulator.
   // Only that host is replaced; WLAN addresses and real host names stay unchanged.
   fun resolve(url: String?): String? {
      if (url == null) return null

      val uri = URI(url)
      if (uri.host != "localhost") return url

      return URI(
         uri.scheme,
         uri.userInfo,
         AVD_HOST,
         uri.port,
         uri.path,
         uri.query,
         uri.fragment,
      ).toString()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Persistierte ImageUrls stammen vom Server und werden grundsätzlich unverändert
 *   verwendet. Nur localhost besitzt im Emulator eine andere Bedeutung.
 * - Die Ersetzung betrifft ausschließlich den Hostnamen. Scheme, Port, Pfad,
 *   Query und Fragment bleiben erhalten.
 * - Eine WLAN-IP oder ein produktiver Hostname wird deshalb niemals überschrieben.
 */
