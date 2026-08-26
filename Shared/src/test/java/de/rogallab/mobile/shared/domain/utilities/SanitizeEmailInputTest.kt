package de.rogallab.mobile.shared.domain.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class SanitizeEmailInputTest {

   @Test
   fun allowedAsciiCharacters_arePreserved() {
      val input = "Ada.Lovelace+test_1@example-domain.org"

      assertEquals(
         input,
         sanitizeEmailInput(input)
      )
   }

   @Test
   fun umlautsAndSharpS_areTransliterated() {
      assertEquals(
         "Mueller.Groesse@example.de",
         sanitizeEmailInput("Müller.Größe@example.de")
      )
   }

   @Test
   fun uppercaseUmlauts_areTransliteratedWithUppercaseBase() {
      assertEquals(
         "AeOeUe@example.de",
         sanitizeEmailInput("ÄÖÜ@example.de")
      )
   }

   @Test
   fun combiningDiaeresis_isConverted() {
      val input = "a\u0308o\u0308u\u0308@example.de"

      assertEquals(
         "aeoeue@example.de",
         sanitizeEmailInput(input)
      )
   }

   @Test
   fun unsupportedDiacritics_areRemovedByNormalization() {
      assertEquals(
         "rene@example.de",
         sanitizeEmailInput("rené@example.de")
      )
   }

   @Test
   fun disallowedCharacters_areRemoved() {
      assertEquals(
         "adaexample.org",
         sanitizeEmailInput(" ada!#example.org ")
      )
   }

   @Test
   fun emptyInput_returnsEmptyString() {
      assertEquals("", sanitizeEmailInput(""))
   }
}
