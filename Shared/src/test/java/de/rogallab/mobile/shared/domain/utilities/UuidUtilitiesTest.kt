package de.rogallab.mobile.shared.domain.utilities

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UuidUtilitiesTest {

   @Test
   fun newUuid_returnsValidUuidString() {
      val uuid = newUuid()

      assertEquals(uuid, UUID.fromString(uuid).toString())
   }

   @Test
   fun newUuid_returnsDifferentValues() {
      assertNotEquals(newUuid(), newUuid())
   }

   @Test
   fun emptyUuid_returnsAllZeroUuid() {
      assertEquals(
         "00000000-0000-0000-0000-000000000000",
         emptyUuid()
      )
   }

   @Test
   fun as8_shortString_isReturnedUnchanged() {
      assertEquals("1234567", "1234567".as8())
   }

   @Test
   fun as8_eightOrMoreCharacters_isShortened() {
      assertEquals("12345678...", "12345678".as8())
      assertEquals("abcdefgh...", "abcdefghijklmnopqrstuvwxyz".as8())
   }

   @Test
   fun createUuid_formatsNumberAndValue() {
      assertEquals(
         "00000042-0007-0000-0000-000000000000",
         createUuid(number = 42, value = 7)
      )
   }
}
