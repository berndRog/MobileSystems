package de.rogallab.mobile.data.remote.dtos

import kotlinx.serialization.Serializable

@Serializable
data class PersonDto(
   val id: String,
   val firstName: String,
   val lastName: String,
   val email: String? = null,
   val phone: String? = null,
   val imageUrl: String? = null,
)
