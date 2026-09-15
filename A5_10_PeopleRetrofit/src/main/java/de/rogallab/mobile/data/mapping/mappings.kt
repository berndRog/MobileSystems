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

fun Person.toPersonDto(): PersonDto =
   PersonDto(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imageUrl = imagePath,
   )

/*
 * Didaktik und Lernziele
 *
 * - Die Mapping-Funktion verhindert, dass Retrofit-/JSON-DTOs die Data-Schicht
 *   verlassen. ViewModels und UI arbeiten weiterhin nur mit Person.
 * - Die beiden Richtungen machen die Grenze ausdrücklich sichtbar: Person ist
 *   das Domain-Modell, PersonDto der JSON-Vertrag der PeopleApi.
 * - Person.imagePath wird auf PersonDto.imageUrl abgebildet und zurück. Ein
 *   lokaler Android-Pfad ist für die API nur Text ohne Dateibedeutung.
 */
