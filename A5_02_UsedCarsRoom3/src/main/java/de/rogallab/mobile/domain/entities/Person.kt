package de.rogallab.mobile.domain.entities

import de.rogallab.mobile.domain.utilities.normalizedImagePath

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

   val validImagePath: String?
      get() = imagePath.normalizedImagePath()
}
