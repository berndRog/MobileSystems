package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person =
   Person(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imageUrl,
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
 * - Die Mapping-Funktionen verhindern, dass Transporttypen die Data-Schicht
 *   verlassen. ViewModels und UI arbeiten weiterhin nur mit Person.
 * - imageUrl ist der JSON-Name der API, imagePath bleibt der bekannte Name im
 *   Android-Domainmodell.
 */
