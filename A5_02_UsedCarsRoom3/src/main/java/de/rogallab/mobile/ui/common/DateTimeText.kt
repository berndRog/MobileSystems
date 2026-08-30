package de.rogallab.mobile.ui.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.*

object DateTimeText {
   const val pattern = "dd.MM.yyyy HH:mm"

   private val formatter = LocalDateTime.Format {
      day()
      char('.')
      monthNumber()
      char('.')
      year()
      char(' ')
      hour()
      char(':')
      minute()
   }

   fun format(value: LocalDateTime?): String =
      value?.let(formatter::format).orEmpty()

   fun parse(value: String): LocalDateTime =
      LocalDateTime.parse(value.trim(), formatter)

   fun parseOrNull(value: String): LocalDateTime? {
      val normalizedValue = value.trim()
      if (normalizedValue.isBlank()) return null
      return LocalDateTime.parseOrNull(normalizedValue, formatter)
   }
}
