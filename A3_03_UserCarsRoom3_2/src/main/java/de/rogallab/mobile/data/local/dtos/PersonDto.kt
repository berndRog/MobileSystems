package de.rogallab.mobile.data.local.dtos

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// Persistence model for one person.
@Entity(tableName = "Person")
data class PersonDto(
   @PrimaryKey val id: String,
   val firstName: String,
   val lastName: String,
   val email: String?,
   val phone: String?,
   val imagePath: String?,
)
