package de.rogallab.mobile

object Globals {
   // adb reverse tcp:5080 tcp:5080 forwards this emulator loopback port to the Mac.
   const val baseUrl = "http://127.0.0.1:5080/"

   // Person.imagePath refers to files in this private Android directory.
   const val imageDirectoryName = "people510"

   const val animationDuration = 1000
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit benötigt eine Base-URL, die mit '/' endet.
 * - A5_10 verwendet adb reverse. Dadurch kann der Android-Client die lokale
 *   PeopleApi über 127.0.0.1:5080 ansprechen, obwohl die API auf dem Mac läuft.
 * - Das lokale Bildverzeichnis enthält persistente Picker- und Seed-Dateien.
 *   PeopleApi speichert nur deren Pfad als String und kennt die Dateien nicht.
 */
