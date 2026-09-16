package de.rogallab.mobile

object Globals {
   // adb reverse tcp:5081 tcp:5081 forwards this emulator loopback port to the Mac.
   const val baseUrl = "http://127.0.0.1:5081/"

   // New picker images remain here only until PeopleImagesApi has stored them.
   const val imageDirectoryName = "people511"

   const val animationDuration = 1000
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit benötigt eine Base-URL, die mit '/' endet.
 * - A5_11 verwendet adb reverse. Dadurch kann der Android-Client die lokale
 *   PeopleImagesApi über 127.0.0.1:5081 ansprechen, obwohl sie auf dem Mac läuft.
 * - Das lokale Bildverzeichnis enthält nur temporäre Picker- und Seed-Dateien.
 *   Nach dem Upload liefert PeopleImagesApi eine dauerhaft erreichbare ImageUrl.
 */
