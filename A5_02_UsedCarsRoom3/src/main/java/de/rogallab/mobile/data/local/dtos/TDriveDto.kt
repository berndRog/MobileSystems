package de.rogallab.mobile.data.local.dtos

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// TDrive is the association entity between Person and Car.
@Entity(
   tableName = "TDrive",
   foreignKeys = [
      ForeignKey(
         entity = PersonDto::class,
         parentColumns = ["id"],
         childColumns = ["personId"],
         onDelete = ForeignKey.RESTRICT,
      ),
      ForeignKey(
         entity = CarDto::class,
         parentColumns = ["id"],
         childColumns = ["carId"],
         onDelete = ForeignKey.RESTRICT,
      ),
   ],
   indices = [
      Index(value = ["personId"]),
      Index(value = ["carId"]),
      Index(value = ["personId", "carId", "start"], unique = true),
   ],
)
data class TDriveDto(
   @PrimaryKey val id: String,
   val personId: String,
   val carId: String,
   val start: String,
   val notes: String?,
   val isCompleted: Boolean,
)
