package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.utilities.newUuid

@Immutable
data class PersonForm(
   val id: String = newUuid(),
   val firstName: String = "",
   val lastName: String = "",
   val email: String = "",
   val phone: String = "",
   val imagePath: String? = null,
)