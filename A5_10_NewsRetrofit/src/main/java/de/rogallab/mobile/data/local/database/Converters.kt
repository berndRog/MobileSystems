package de.rogallab.mobile.data.local.database

import androidx.room.TypeConverter
import de.rogallab.mobile.data.dtos.Source

class Converters {

   @TypeConverter
   fun fromSource(source: Source): String? =
      source.name

   @TypeConverter
   fun toSource(name: String?): Source {
      var source = Source("","")
      name?.let {
         source = Source(name, name)
      }
      return source
   }
}