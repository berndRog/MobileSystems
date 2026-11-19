package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import app.cash.turbine.test
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import kotlin.coroutines.cancellation.CancellationException
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

      assertEquals(Unit, repository.create(person).getOrThrow())
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertTrue(repository.update(person).isFailure)
      assertTrue(repository.remove(person).isFailure)
      assertEquals(person, repository.findById(person.id).getOrThrow())

      dao.updateFailure = null
      dao.deleteFailure = null
      assertEquals(Unit, repository.update(person).getOrThrow())
      assertEquals(Unit, repository.remove(person).getOrThrow())
      assertEquals(null, repository.findById(person.id).getOrThrow())
   }

   @Test
   fun create_existingId_returnsFailureAndKeepsOriginal() = runTest {
      val repository = PersonRepository(FakePersonDao())
      val person = Person(id = "p1", firstName = "Ada", lastName = "Lovelace")
      assertEquals(Unit, repository.create(person).getOrThrow())

      assertTrue(repository.create(person.copy(firstName = "Changed")).isFailure)
      assertEquals(person, repository.findById(person.id).getOrThrow())
   }

   @Test
   fun updateAndRemove_unexpectedRowCounts_returnFailures() = runTest {
      val person = Person(id = "missing")
      for (rows in listOf(-1, 0, 2)) {
         val dao = object : IPersonDao by FakePersonDao() {
            override suspend fun update(personDto: PersonDto): Int = rows
            override suspend fun delete(personDto: PersonDto): Int = rows
         }
         val repository = PersonRepository(dao)
         assertTrue(repository.update(person).exceptionOrNull() is IllegalStateException)
         assertTrue(repository.remove(person).exceptionOrNull() is IllegalStateException)
      }
   }

   @Test
   fun writeOperations_rethrowCancellation() = runTest {
      val cancellation = CancellationException("write cancelled")
      val dao = FakePersonDao().apply {
         insertFailure = cancellation
         updateFailure = cancellation
         deleteFailure = cancellation
      }
      val repository = PersonRepository(dao)
      val person = Person(id = "p1")
      val operations: List<suspend () -> Result<Unit>> = listOf(
         { repository.create(person) },
         { repository.update(person) },
         { repository.remove(person) },
      )
      for (operation in operations) {
         try {
            operation()
            fail("CancellationException must propagate")
         } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
         }
      }
   }

   @Test
   fun create_invalidRowId_returnsFailure() = runTest {
      val person = Person(id = "p1")
      val dao = object : IPersonDao by FakePersonDao() {
         override suspend fun insert(personDto: PersonDto): Long = -1L
      }
      assertTrue(PersonRepository(dao).create(person).exceptionOrNull() is IllegalStateException)
   }

   @Test
   fun findById_missing_returnsSuccessWithNull() = runTest {
      assertEquals(null, PersonRepository(FakePersonDao()).findById("missing").getOrThrow())
   }

   @Test
   fun findById_handlesExceptionsAndPropagatesCancellation() = runTest {
      val failure = IllegalStateException("read failed")
      val cancellation = CancellationException("read cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : IPersonDao by FakePersonDao() {
            override suspend fun findById(personId: String): PersonDto? = throw throwable
         }
         val repository = PersonRepository(dao)
         if (throwable is CancellationException) {
            try {
               repository.findById("id")
               fail("CancellationException must propagate")
            } catch (actual: CancellationException) {
               assertSame(cancellation, actual)
            }
         } else {
            assertSame(failure, repository.findById("id").exceptionOrNull())
         }
      }
   }

   @Test
   fun observeAll_handlesExceptionsAndPropagatesCancellation() = runTest {
      val failure = IllegalStateException("observe failed")
      val cancellation = CancellationException("observe cancelled")
      for (throwable in listOf(failure, cancellation)) {
         val dao = object : IPersonDao by FakePersonDao() {
            override fun observeAll() = flow<List<PersonDto>> { throw throwable }
         }
         val repository = PersonRepository(dao)
         if (throwable is CancellationException) {
            try {
               repository.observeAll().first()
               fail("CancellationException must propagate")
            } catch (actual: CancellationException) {
               assertSame(cancellation, actual)
            }
         } else {
            assertSame(failure, repository.observeAll().first().exceptionOrNull())
         }
      }
   }
}
