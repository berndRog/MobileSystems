package de.rogallab.mobile.domain.formmodels

import de.rogallab.mobile.domain.utilities.newUuid

data class PersonForm(
   val firstName: String = "",
   val lastName: String = "",
   val email: String = "",
   val phone: String = "",
   val imagePath: String? = null,
)