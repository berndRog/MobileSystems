package de.rogallab.mobile.shared.data.local.database

import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDateTimeConvertersTest {

   @Test
   fun localDateTimeToIsoString_nullReturnsNull() {
      assertNull(
         LocalDateTimeConverters.localDateTimeToIsoString(null)
      )
   }

   @Test
   fun isoStringToLocalDateTime_nullReturnsNull() {
      assertNull(
         LocalDateTimeConverters.isoStringToLocalDateTime(null)
      )
   }

   @Test
   fun localDateTime_roundTripPreservesValue() {
      val dateTime =
         LocalDateTime.parse("2026-02-14T12:30:45")

      val isoString =
         LocalDateTimeConverters.localDateTimeToIsoString(dateTime)

      val result =
         LocalDateTimeConverters.isoStringToLocalDateTime(isoString)

      assertEquals(dateTime, result)
   }

   @Test
   fun localDateTimeToIsoString_returnsInstantRepresentation() {
      val dateTime =
         LocalDateTime.parse("2026-01-15T10:00:00")

      val isoString =
         LocalDateTimeConverters.localDateTimeToIsoString(dateTime)

      assertTrue(isoString?.endsWith("Z") == true)
   }
}
