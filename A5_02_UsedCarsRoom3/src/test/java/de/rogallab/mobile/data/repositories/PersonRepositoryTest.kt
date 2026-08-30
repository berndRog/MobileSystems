package de.rogallab.mobile.data.repositories

import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonRepositoryTest {

   @Test
   fun create_emitsPersistedPerson() = runTest {
      val repository = PersonRepository(FakePersonDao())
      val person = Person(
         id = "p-1",
         firstName = "Ada",
         lastName = "Lovelace",
         email = "ada.lovelace@example.org",
         phone = "+44 20 7946 0101",
         imagePath = "/images/ada.jpg",
      )

      repository.observeAll().test {
         assertTrue(awaitItem().getOrThrow().isEmpty())

         repository.create(person).getOrThrow()

         assertEquals(
            listOf(person),
            awaitItem().getOrThrow()
         )

         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun updateUnknownPerson_returnsFailure() = runTest {
      val repository = PersonRepository(FakePersonDao())

      val result = repository.update(
         Person(
            id = "missing",
            firstName = "Missing",
            lastName = "Person"
         )
      )

      assertTrue(result.isFailure)
   }
}
