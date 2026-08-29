package de.rogallab.mobile.domain.entities

data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = null,
   val phone: String? = null,
   val imagePath: String? = null,
   val id: String,
) {
   val displayName: String
      get() = "$firstName $lastName".trim()
}
