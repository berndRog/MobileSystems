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
 * - imagePath ist in A5_11 eine Bildreferenz: Nach dem Laden enthält sie die
 *   persistente Server-URL; während einer noch nicht gespeicherten Bildauswahl
 *   kann sie vorübergehend einen lokalen privaten Dateipfad enthalten.
 * - Das Repository erkennt diese beiden Fälle und baut daraus den passenden
 *   multipart/form-data-Request für PeopleApi.
 */
