package de.rogallab.mobile.data.local.dtos

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// A persisted car belongs to exactly one seller.
@Entity(
   tableName = "Car",
   foreignKeys = [
      ForeignKey(
         entity = PersonDto::class,
         parentColumns = ["id"],
         childColumns = ["personId"],
         onDelete = ForeignKey.RESTRICT,
      )
   ],
   indices = [Index(value = ["personId"])],
)
data class CarDto(
   @PrimaryKey val id: String,
   val manufacturer: String,
   val model: String,
   val price: Int?,
   @ColumnInfo(defaultValue = "'[]'")
   val imagePaths: List<String> = emptyList(),
   val personId: String?,        // seller
)
