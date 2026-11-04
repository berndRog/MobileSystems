package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonMappingsTest {
   @Test fun person_roundTripThroughDatabaseDtoPreservesVisibleFieldsAndId() {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")

      val restored = person.toPersonDto().toPerson()

      assertEquals(person.id, restored.id)
      assertEquals(person.firstName, restored.firstName)
      assertEquals(person.lastName, restored.lastName)
   }
}
