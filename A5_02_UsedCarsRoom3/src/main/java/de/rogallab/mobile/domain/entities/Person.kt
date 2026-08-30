package de.rogallab.mobile.domain.entities

import de.rogallab.mobile.shared.domain.utilities.newUuid

data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = "",
   val phone: String? = "",
   val imagePath: String? = null,
   val id: String = newUuid(),
) {
   val fullName: String get() = "$firstName $lastName".trim()
   val displayName: String get() = fullName
}
