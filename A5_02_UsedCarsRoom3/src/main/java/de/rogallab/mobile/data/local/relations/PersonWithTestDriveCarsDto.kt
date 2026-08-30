package de.rogallab.mobile.data.local.relations

import androidx.room3.Embedded
import androidx.room3.Junction
import androidx.room3.Relation
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto

// Result model for the many-to-many relationship Person <-> Car.
//
// TDrive is used as the junction entity and still remains a full entity
// because it stores its own date, notes and completion state.
data class PersonWithTestDriveCarsDto(
   @Embedded val person: PersonDto,
   @Relation(
      parentColumns = ["id"],
      entityColumns = ["id"],
      associateBy = Junction(
         value = TDriveDto::class,
         parentColumns = ["personId"],
         entityColumns = ["carId"],
      ),
   )
   val cars: List<CarDto>,
)
