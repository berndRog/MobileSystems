package de.rogallab.mobile.domain.entities

import de.rogallab.mobile.shared.domain.utilities.newUuid

data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = "",
   val phone: String? = "",
   val imagePath: String? = null,
   val id: String = newUuid(),
) {
   val fullName: String
      get() = "$firstName $lastName".trim()
}

/*
 * Didaktik und Lernziele
 *
 * - Person bleibt gegenüber A5_01 weitgehend unverändert, damit der Lernschritt
 *   auf Room -> Retrofit fokussiert bleibt.
 * - imagePath bleibt das bekannte nullable String-Feld. Während der Bearbeitung
 *   kann es einen lokalen Android-Pfad enthalten; nach erfolgreichem Speichern
 *   enthält es die von PeopleImagesApi zurückgegebene Server-URL.
 */
