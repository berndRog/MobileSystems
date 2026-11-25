package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.people.create_detail.PersonForm
import org.junit.Assert.*
import org.junit.Test

class PersonFormMappingTest {
   @Test fun domainToForm_preservesIdentityAndMapsNullsToEmptyInput() {
      val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = null, phone = null, imagePath = "/images/ada.jpg")
      assertEquals(PersonForm(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = "", phone = "", imagePath = "/images/ada.jpg"), person.toPersonForm())
   }

   @Test fun formToDomain_mapsBlankOptionalInputToNull_andPreservesImage() {
      val form = PersonForm(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = "  ", phone = "", imagePath = "/images/ada.jpg")
      assertEquals(Person(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = null, phone = null, imagePath = "/images/ada.jpg"), form.toPerson())
   }

   @Test fun normalization_trimsAndSanitizes_aCopyOfTheRawInput() {
      val raw = PersonForm(id = "p1", firstName = "  Ada ", lastName = " Lovelace  ",
         email = "  ada@example.org  ", phone = "  +49abc (123) 45-6  ",
         imagePath = "/images/ada.jpg")
      val normalized = raw.normalized()
      assertEquals(PersonForm(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = "ada@example.org", phone = "+49 (123) 45-6",
         imagePath = "/images/ada.jpg"), normalized)
      assertEquals("  Ada ", raw.firstName)
      assertEquals("  +49abc (123) 45-6  ", raw.phone)
      assertEquals(normalized, normalized.normalized())
   }

   @Test fun populatedEntity_roundTripsWithoutChangingItsFields() {
      val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace",
         email = "ada@example.org", phone = "+44 1234", imagePath = "/images/ada.jpg")
      assertEquals(person, person.toPersonForm().toPerson())
   }
}
