package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.data.remote.network.ServerUrl
import de.rogallab.mobile.domain.entities.Person

fun PersonDto.toPerson(): Person =
   Person(
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = ServerUrl.resolve(imageUrl),
      id = id,
   )

/*
 * Didaktik und Lernziele
 *
 * - Die Mapping-Funktion verhindert, dass Retrofit-/JSON-DTOs die Data-Schicht
 *   verlassen. ViewModels und UI arbeiten weiterhin nur mit Person.
 * - Die vom Server gelieferte ImageUrl wird beim Mapping für den Android Emulator
 *   aufgelöst, falls ihr Host localhost ist.
 */
