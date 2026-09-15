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
 * - imagePath bleibt die lokale Bildreferenz aus den vorherigen Beispielen. Ihr
 *   Wert wird von Retrofit als normaler nullable String übertragen.
 * - PeopleApi speichert diesen Wert zusammen mit den übrigen Eigenschaften und
 *   empfängt oder verarbeitet keine Bilddatei.
 */
