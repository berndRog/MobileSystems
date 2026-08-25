package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonFormMappingTest {

   @Test
   fun normalized_trimsValuesAndConvertsBlankOptionalValuesToNull() {
      val person = Person(
         firstName = "  Ada  ",
         lastName = "  Lovelace ",
         email = "   ",
         phone = "  +49 123456  ",
      )

      val normalized = person.normalized()

      assertEquals("Ada", normalized.firstName)
      assertEquals("Lovelace", normalized.lastName)
      assertNull(normalized.email)
      assertEquals("+49 123456", normalized.phone)
   }
}
