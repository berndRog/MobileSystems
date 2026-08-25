package de.rogallab.mobile.shared.data.local.database

import androidx.room3.ColumnTypeConverter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object LocalDateTimeConverters {

   private val localTimeZone: TimeZone
      get() = TimeZone.currentSystemDefault()

   @ColumnTypeConverter
   fun localDateTimeToIsoString(dateTime: LocalDateTime?): String? =
      dateTime
         ?.toInstant(localTimeZone)
         ?.toString()

   @ColumnTypeConverter
   fun isoStringToLocalDateTime(isoString: String?): LocalDateTime? =
      isoString
         ?.let { value -> Instant.parse(value) }
         ?.toLocalDateTime(localTimeZone)
}