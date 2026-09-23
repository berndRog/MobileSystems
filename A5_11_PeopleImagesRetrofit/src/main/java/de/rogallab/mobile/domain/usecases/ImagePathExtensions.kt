package de.rogallab.mobile.domain.usecases

// Returns true only for a file path owned by the Android app.
internal fun String?.isLocalImagePath(): Boolean =
   !isNullOrBlank() &&
      !startsWith("http://", ignoreCase = true) &&
      !startsWith("https://", ignoreCase = true)

/*
 * Didaktik und Lernziele
 *
 * - Ein gemeinsames Prädikat verhindert, dass Create, Update und Delete
 *   unterschiedliche Regeln zur Erkennung lokaler Bilddateien verwenden.
 *
 * - HTTP(S)-URLs bezeichnen Ressourcen der PeopleImagesApi; alle anderen nicht
 *   leeren Werte werden in diesem Beispiel als lokale App-Dateipfade behandelt.
 *
 * Lernziele:
 *
 * - Wiederholte fachliche Regeln an einer Stelle ausdrücken.
 * - Client-Dateien und Serverressourcen anhand ihrer Referenz unterscheiden.
 */
