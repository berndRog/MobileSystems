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

fun Person.toPersonListItem(): de.rogallab.mobile.domain.readmodels.PersonListItem =
   de.rogallab.mobile.domain.readmodels.PersonListItem(
      id = id,
      fullName = "$firstName $lastName",
      email = email,
      phone = phone,
      imagePath = imagePath,
   )
