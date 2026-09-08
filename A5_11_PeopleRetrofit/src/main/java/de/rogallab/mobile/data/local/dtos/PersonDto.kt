package de.rogallab.mobile.data.local.dtos

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "Person")
data class PersonDto(
   @PrimaryKey
   val id: String,
   val firstName: String,
   val lastName: String,
   val email: String?,
   val phone: String?,
   val imagePath: String?,
)

/*
 * Didaktik und Lernziele
 *
 * - PersonDto ist das persistente Room-Modell. Die Room-Annotationen bleiben
 *   damit aus der Domain-Entity Person heraus.
 *
 * - Repository und Mapping bilden die Grenze zwischen Persistenz- und
 *   Domain-Schicht.
 */
