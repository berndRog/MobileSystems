package de.rogallab.mobile.shared.data.local.database

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant

object InstantConverters {
   @ColumnTypeConverter
   fun instantToIsoString(instant: Instant?): String? =
      instant?.toString()

   @ColumnTypeConverter
   fun isoStringToInstant(isoString: String?): Instant? =
      isoString?.let(Instant::parse)
}
