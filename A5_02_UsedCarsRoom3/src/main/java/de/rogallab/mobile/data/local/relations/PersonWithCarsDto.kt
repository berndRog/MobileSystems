package de.rogallab.mobile.data.local.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto

// Result model for the one-to-many relationship Person -> Cars.
data class PersonWithCarsDto(
   @Embedded val person: PersonDto,
   @Relation(
      parentColumns = ["id"],
      entityColumns = ["sellerId"],
   )
   val cars: List<CarDto>,
)
