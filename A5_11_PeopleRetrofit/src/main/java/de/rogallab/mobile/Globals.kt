package de.rogallab.mobile

object Globals {
   // adb reverse tcp:5080 tcp:5080 forwards this emulator loopback port to the Mac.
   const val baseUrl = "http://127.0.0.1:5080/"

   // Local image files are only temporary upload files in A5_11.
   const val imageDirectoryName = "people511"

   const val animationDuration = 1000
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit benötigt eine Base-URL, die mit '/' endet.
 * - A5_11 verwendet adb reverse. Dadurch kann der Android-Client die lokale
 *   PeopleApi über 127.0.0.1:5080 ansprechen, obwohl die API auf dem Mac läuft.
 * - Auch vom Server erzeugte ImageUrls verwenden denselben Host und benötigen
 *   deshalb keine zusätzliche 10.0.2.2-Umschreibung im Client.
 * - Das lokale Bildverzeichnis enthält nur temporäre Picker- und Seed-Dateien.
 *   Persistente Bilder werden von PeopleApi gespeichert und als URL geliefert.
 */
