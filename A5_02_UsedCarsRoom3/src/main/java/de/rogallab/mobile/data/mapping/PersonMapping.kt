package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person =
   Person(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imagePath,
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
 * - Die technische Verwaltung der Bilddateien gehört zur Shared-
 *   Infrastruktur. Das Mapping übernimmt deshalb den gespeicherten Pfad
 *   unverändert zwischen DTO und Domain-Entity.
 */
