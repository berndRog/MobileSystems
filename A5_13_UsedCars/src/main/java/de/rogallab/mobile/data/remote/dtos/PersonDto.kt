package de.rogallab.mobile.data.remote.dtos

data class PersonDto(
   val id: String,
   val firstName: String,
   val lastName: String,
   val email: String?,
   val phone: String?,
   val imageUrl: String?,
)
