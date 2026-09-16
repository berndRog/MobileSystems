package de.rogallab.mobile.shared.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class InstantConvertersTest {

   @Test
   fun instantToIsoString_nullReturnsNull() {
      assertNull(
         InstantConverters.instantToIsoString(null)
      )
   }

   @Test
   fun isoStringToInstant_nullReturnsNull() {
      assertNull(
         InstantConverters.isoStringToInstant(null)
      )
   }

   @Test
   fun instant_roundTripPreservesValue() {
      val instant =
         Instant.parse("2026-02-14T12:30:45Z")

      val isoString =
         InstantConverters.instantToIsoString(instant)

      val result =
         InstantConverters.isoStringToInstant(isoString)

      assertEquals(instant, result)
   }

   @Test
   fun instantToIsoString_returnsUtcRepresentation() {
      val instant =
         Instant.parse("2026-01-15T10:00:00Z")

      assertEquals(
         "2026-01-15T10:00:00Z",
         InstantConverters.instantToIsoString(instant),
      )
   }
}
