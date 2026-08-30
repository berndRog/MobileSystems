package de.rogallab.mobile.ui.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeText {
   const val pattern = "dd.MM.yyyy HH:mm"
   private val formatter = DateTimeFormatter.ofPattern(pattern)

   fun format(value: LocalDateTime?): String =
      value?.format(formatter).orEmpty()

   fun parse(value: String): LocalDateTime =
      LocalDateTime.parse(value.trim(), formatter)

   fun parseOrNull(value: String): LocalDateTime? {
      val normalizedValue = value.trim()
      if (normalizedValue.isBlank()) return null
      return try {
         LocalDateTime.parse(normalizedValue, formatter)
      }
      catch (_: DateTimeParseException) {
         null
      }
   }
}
