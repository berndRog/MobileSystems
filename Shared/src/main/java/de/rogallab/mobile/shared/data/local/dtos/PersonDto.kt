package de.rogallab.mobile.shared.data.local.dtos

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import de.rogallab.mobile.shared.domain.utilities.newUuid

@Entity(
   tableName="Person"
)
data class PersonDto (
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = null,
   val phone: String? = null,
   val imagePath: String? = null,
   @PrimaryKey
   val id: String = newUuid()  // Uuid
)