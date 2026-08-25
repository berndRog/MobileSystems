package de.rogallab.mobile.domain.entities

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val id: String = UUID.randomUUID().toString()
)
