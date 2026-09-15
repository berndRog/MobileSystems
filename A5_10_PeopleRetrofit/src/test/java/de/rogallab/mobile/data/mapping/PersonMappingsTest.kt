package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonMappingsTest {

   @Test
   fun personToPersonDto_mapsLocalImagePathToImageUrlString() {
      val person = Person(
         firstName = "Ada",
         lastName = "Lovelace",
         email = "ada@example.org",
         phone = "12345",
         imagePath = "/data/user/0/de.rogallab.mobile/files/people510/ada.jpg",
         id = "p1",
      )

      val dto = person.toPersonDto()

      assertEquals(person.firstName, dto.firstName)
      assertEquals(person.lastName, dto.lastName)
      assertEquals(person.email, dto.email)
      assertEquals(person.phone, dto.phone)
      assertEquals(person.imagePath, dto.imageUrl)
      assertEquals(person.id, dto.id)
   }

   @Test
   fun personDtoToPerson_preservesNullImageReference() {
      val dto = PersonDto(
         id = "p2",
         firstName = "Grace",
         lastName = "Hopper",
         email = null,
         phone = null,
         imageUrl = null,
      )

      val person = dto.toPerson()

      assertNull(person.imagePath)
      assertEquals(dto.id, person.id)
   }
}
