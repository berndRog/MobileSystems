package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person =
   Person(
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imageUrl,
      id = id,
   )

/*
 * Didaktik und Lernziele
 *
 * - Die Mapping-Funktion verhindert, dass Retrofit-/JSON-DTOs die Data-Schicht
 *   verlassen. ViewModels und UI arbeiten weiterhin nur mit Person.
 * - Mit adb reverse verwendet die App 127.0.0.1 auch für vom Server erzeugte
 *   ImageUrls. Eine zusätzliche Host-Umschreibung auf 10.0.2.2 ist nicht nötig.
 */
