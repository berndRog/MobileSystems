package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.normalizedImagePath

fun PersonDto.toPerson(): Person =
   Person(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imagePath.normalizedImagePath(),
   )

fun Person.toPersonDto(): PersonDto =
   PersonDto(
      id = id,
      firstName = firstName,
      lastName = lastName,
      email = email,
      phone = phone,
      imagePath = imagePath.normalizedImagePath(),
   )
