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
         childColumns = ["sellerId"],
         onDelete = ForeignKey.RESTRICT,
      )
   ],
   indices = [Index(value = ["sellerId"])],
)
data class CarDto(
   @PrimaryKey val id: String,
   val manufacturer: String,
   val model: String,
   val registrationYear: Int?,
   val mileage: Int?,
   val priceInEuro: Int?,
   val sellerId: String,
   @ColumnInfo(defaultValue = "'[]'")
   val imagePaths: List<String> = emptyList(),
)
