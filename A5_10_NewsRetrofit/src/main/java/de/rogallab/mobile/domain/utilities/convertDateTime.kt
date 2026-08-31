package de.rogallab.mobile.domain.utilities


import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// locale time zone
// TimeZone.currentSystemDefault()
// UTC time zone
// TimeZone.UTC


// convert LocalDateTime to Iso String
@OptIn(ExperimentalTime::class)
fun LocalDateTime.toIsoUtcString(): String {
   return this.toInstant(TimeZone.UTC).toString()
}

// convert LocalDateTime to Iso Utc String
@OptIn(ExperimentalTime::class)
fun LocalDateTime.toIsoString(): String {
   return this.toInstant(TimeZone.currentSystemDefault()).toString()
}

@OptIn(ExperimentalTime::class)
fun toLocalDateTimeFromIso(isoString:String): LocalDateTime {
   return Instant.parse(isoString)
                 .toLocalDateTime(TimeZone.currentSystemDefault())
}

// covert LocalDateTime to another Tome.Zone
@OptIn(ExperimentalTime::class)
fun LocalDateTime.toTimeZone(timeZone: TimeZone): LocalDateTime {
   val instant = this.toInstant(TimeZone.currentSystemDefault())
   return instant.toLocalDateTime(timeZone)
}

// -----------------------------------------------------------------------------
// LocalDateTime  Kotlin
// -----------------------------------------------------------------------------
// to Date
fun LocalDateTime.toDateString(
   locale: Locale = Locale.getDefault()
): String {
   val dts: DateTimeString = this.formatted()
   return when (locale.language) {
      "de" -> "${dts.day}.${dts.month}.${dts.year}"
      "en" -> "${dts.month}/${dts.day}/${dts.year}"
      else -> "${dts.day}.${dts.month}.${dts.year}"
   }
}

// to Time
fun LocalDateTime.toTimeString(): String {
   val dts: DateTimeString = this.formatted()
   return "${dts.hour}:${dts.min}:${dts.sec}"
}

// To DateTime
fun LocalDateTime.toDateTimeString(
   locale: Locale = Locale.getDefault(),
): String {
   val dts: DateTimeString = this.formatted()
   return when (locale.language) {
      "de" -> "${dts.day}.${dts.month}.${dts.year} ${dts.hour}:${dts.min}:${dts.sec}"
      "en" -> "${dts.month}/${dts.day}/${dts.year} ${dts.hour}:${dts.min}:${dts.sec}"
      else -> "${dts.day}.${dts.month}.${dts.year} ${dts.hour}:${dts.min}:${dts.sec}"
   }
}

private fun LocalDateTime.formatted(): DateTimeString =
   DateTimeString(
      year = this.date.year.toString(),
      month = this.date.monthNumber.toString().padStart(2, '0'),
      day = this.date.dayOfMonth.toString().padStart(2, '0'),
      dayOfWeek = this.date.dayOfWeek.name,
      hour = this.time.hour.toString().padStart(2, '0'),
      min = this.time.minute.toString().padStart(2, '0'),
      sec = this.time.second.toString().padStart(2, '0'),
      mil = (this.time.nanosecond / 1_000_000).toString().padStart(3, '0')
   )


private data class DateTimeString(
   val year: String,
   val month: String,
   val day: String,
   val dayOfWeek: String,
   val hour: String,
   val min: String,
   val sec: String,
   val mil: String
)
