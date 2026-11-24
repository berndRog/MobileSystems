package de.rogallab.mobile.data.local.database

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Converts a list of private image paths to one JSON database column.
object StringListColumnConverter {
   @ColumnTypeConverter
   fun fromJson(value: String): List<String> =
      if (value.isBlank()) emptyList()
      else Json.decodeFromString(value)

   @ColumnTypeConverter
   fun toJson(value: List<String>): String =
      Json.encodeToString(value)
}
