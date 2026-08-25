package de.rogallab.mobile.domain.entities

import de.rogallab.mobile.shared.domain.utilities.newUuid

data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = null,
   val phone: String? = null,
   val imagePath: String? = null,
   val id: String = newUuid(),
){
   val fullName:  String = "$firstName $lastName"
}