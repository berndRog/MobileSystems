package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test

class PersonInputMappingTest {

   @Test
   fun normalized_trimsNamesAndKeepsInternalWhitespace() {
      val person = Person(firstName = "  Ada  Maria\t", lastName = "\n von  Lovelace  ")

      val normalized = person.normalized()

      assertEquals("Ada  Maria", normalized.firstName)
      assertEquals("von  Lovelace", normalized.lastName)
   }

   @Test
   fun normalized_sanitizesEmail() {
      val person = Person(email = "  Jörg.Müller+groß@exämple.de!  ")

      assertEquals("Joerg.Mueller+gross@exaemple.de", person.normalized().email)
   }

   @Test
   fun normalized_sanitizesPhoneAfterTrimmingAndKeepsFormatting() {
      val person = Person(phone = "  +49 (170) 12a3/45.6-7+!  ")

      assertEquals("+49 (170) 123/45.6-7", person.normalized().phone)
   }

   @Test
   fun normalized_preservesPhoneLengthLimit() {
      val person = Person(phone = "1".repeat(35))

      assertEquals("1".repeat(30), person.normalized().phone)
   }

   @Test
   fun normalized_mapsNullEmptyAndBlankOptionalInputsToNull() {
      for (input in listOf(null, "", " \t\n ")) {
         val normalized = Person(email = input, phone = input, imagePath = input).normalized()

         assertNull(normalized.email)
         assertNull(normalized.phone)
         assertNull(normalized.imagePath)
      }
   }

   @Test
   fun normalized_mapsInputsEmptiedBySanitizersToNull() {
      val normalized = Person(email = " !?# ", phone = " abc!? ").normalized()

      assertNull(normalized.email)
      assertNull(normalized.phone)
   }

   @Test
   fun normalized_preservesIdAndImagePathAndExistingPathTrimming() {
      val person = Person(id = " person-1 ", imagePath = "/images/original.jpg")

      val normalized = person.normalized()

      assertEquals(person.id, normalized.id)
      assertEquals(person.imagePath, normalized.imagePath)
      assertEquals("/images/original.jpg",
         person.copy(imagePath = "  /images/original.jpg  ").normalized().imagePath)
   }

   @Test
   fun normalized_returnsCopyWithoutChangingOriginal() {
      val person = Person(
         firstName = " Ada ",
         lastName = " Lovelace ",
         email = " müller@example.de! ",
         phone = " +49abc123456 ",
         imagePath = " /images/original.jpg ",
         id = "person-1",
      )
      val original = person.copy()

      val normalized = person.normalized()

      assertNotSame(person, normalized)
      assertEquals(original, person)
      assertEquals("Ada", normalized.firstName)
      assertEquals("mueller@example.de", normalized.email)
      assertEquals("+49123456", normalized.phone)
   }
}
