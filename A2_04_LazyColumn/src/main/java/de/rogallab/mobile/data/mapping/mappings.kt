package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.local.dtos.PersonDto


fun PersonDto.toPerson(): Person = Person(
   firstName = firstName,
   lastName = lastName,
   id = id
)

fun Person.toPersonDto(): PersonDto = PersonDto(
   firstName = firstName,
   lastName = lastName,
   id = id
)


