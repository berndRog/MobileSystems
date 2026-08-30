package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person =
   Person(
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imagePath,
      id = id,
   )

fun Person.toPersonDto(): PersonDto =
   PersonDto(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imagePath,
   )

/*
 * Didaktik und Lernziele
 *
 * - Die Mapping-Funktionen verhindern, dass Room-Typen die Data-Schicht
 *   verlassen. ViewModels und UI arbeiten weiterhin nur mit Person.
 */
