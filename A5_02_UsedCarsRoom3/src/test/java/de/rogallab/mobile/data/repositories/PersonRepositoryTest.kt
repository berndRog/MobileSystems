package de.rogallab.mobile.data.repositories

import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersonRepositoryTest {

   @Before
   fun setup() {
      // Local JVM tests must not call the android.util.Log stub.
      Alog.set(useAndroidLog = false)
   }

   @After
   fun tearDown() {
      Alog.reset()
   }

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
   fun updateAndRemove_returnDaoFailuresAsResults() = runTest {
      val dao = FakePersonDao()
      val repository = PersonRepository(dao)
      val person = Person(
         id = "p-1",
         firstName = "Ada",
         lastName = "Lovelace"
      )

      repository.create(person).getOrThrow()
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertTrue(repository.update(person).isFailure)
      assertTrue(repository.remove(person).isFailure)
      assertEquals(person, repository.findById(person.id).getOrThrow())

      dao.updateFailure = null
      dao.deleteFailure = null
      assertTrue(repository.update(person).isSuccess)
      assertTrue(repository.remove(person).isSuccess)
      assertEquals(null, repository.findById(person.id).getOrThrow())
   }
}
