package de.rogallab.mobile.shared.data.local.dtos

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonDtoTest {

   @Test
   fun defaultValues_createEmptyOptionalFieldsAndValidUuid() {
      val person = PersonDto()

      assertEquals("", person.firstName)
      assertEquals("", person.lastName)
      assertNull(person.email)
      assertNull(person.phone)
      assertNull(person.imagePath)
      assertEquals(person.id, UUID.fromString(person.id).toString())
   }

   @Test
   fun defaultId_isDifferentForDifferentInstances() {
      assertNotEquals(PersonDto().id, PersonDto().id)
   }

   @Test
   fun constructor_preservesAllValues() {
      val person = PersonDto(
         firstName = "Ada",
         lastName = "Lovelace",
         email = "ada@example.org",
         phone = "+49 123",
         imagePath = "/images/ada.jpg",
         id = "person-1",
      )

      assertEquals("Ada", person.firstName)
      assertEquals("Lovelace", person.lastName)
      assertEquals("ada@example.org", person.email)
      assertEquals("+49 123", person.phone)
      assertEquals("/images/ada.jpg", person.imagePath)
      assertEquals("person-1", person.id)
   }
}
