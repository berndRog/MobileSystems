package de.rogallab.mobile.domain.entities

import java.util.UUID

data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val id: UUID = UUID.randomUUID()
)
