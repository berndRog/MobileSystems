package de.rogallab.mobile

object Globals {
   // 10.0.2.2 addresses the host computer from the Android Emulator.
   // On a physical device replace this host with the computer's WLAN address.
   // const val baseUrl = "http://10.0.2.2:5080/"
   const val baseUrl = "http://127.0.0.1:5080/"

   // Local image files are only temporary upload files in A5_11.
   const val imageDirectoryName = "people511"

   const val animationDuration = 1000
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit benötigt eine Base-URL, die mit '/' endet.
 * - Im Android Emulator bezeichnet localhost das Android-Gerät selbst. Der Host-
 *   Rechner ist über 10.0.2.2 erreichbar.
 * - Das lokale Bildverzeichnis enthält in A5_11 nur temporäre Picker-Dateien.
 *   Persistente Bilder werden von PeopleApi gespeichert und als URL geliefert.
 */
