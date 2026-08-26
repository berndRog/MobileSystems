package de.rogallab.mobile.shared.domain.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizePhoneInputTest {

   @Test
   fun digitsAndFormattingCharacters_arePreserved() {
      val input = "+49 (511) 123-45/67.89"

      assertEquals(
         input,
         sanitizePhoneInput(input)
      )
   }

   @Test
   fun plusSign_isAcceptedOnlyAtBeginning() {
      assertEquals(
         "+49123",
         sanitizePhoneInput("+49+123+")
      )
   }

   @Test
   fun plusSignAfterFilteredCharacters_canBecomeFirstOutputCharacter() {
      assertEquals(
         "+49123",
         sanitizePhoneInput("abc+49xyz123")
      )
   }

   @Test
   fun unsupportedCharacters_areRemoved() {
      assertEquals(
         "0511 123456",
         sanitizePhoneInput("Tel:0511 123456")
      )
   }

   @Test
   fun allowedSpaces_arePreserved() {
      assertEquals(
         " 0511 123456",
         sanitizePhoneInput("Tel: 0511 123456")
      )
   }

   @Test
   fun result_isLimitedToThirtyCharacters() {
      val input = "1234567890123456789012345678901234567890"

      assertEquals(
         "123456789012345678901234567890",
         sanitizePhoneInput(input)
      )
   }

   @Test
   fun emptyInput_returnsEmptyString() {
      assertEquals("", sanitizePhoneInput(""))
   }
}
