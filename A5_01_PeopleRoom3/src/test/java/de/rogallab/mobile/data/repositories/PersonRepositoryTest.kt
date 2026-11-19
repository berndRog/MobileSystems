package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PersonRepositoryTest {
   private val person = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")

   private class FakeDao : IPersonDao {
      var insertFailure: Throwable? = null
      var insertRowId = 42L
      var updatedRows = 1
      var deletedRows = 1
      var insertedPerson: PersonDto? = null
      var updateFailure: Throwable? = null
      var deleteFailure: Throwable? = null
      var updateCount = 0
      var deleteCount = 0
      override fun observeAll(): Flow<List<PersonDto>> = flowOf(emptyList())
      override suspend fun findById(id: String): PersonDto? = null
      override suspend fun count(): Int = 0
      override suspend fun insert(personDto: PersonDto): Long {
         insertFailure?.let { throw it }
         insertedPerson = personDto
         return insertRowId
      }
      override suspend fun insert(personDtos: List<PersonDto>) = Unit
      override suspend fun update(personDto: PersonDto): Int {
         updateFailure?.let { throw it }
         updateCount++
         return updatedRows
      }
      override suspend fun delete(personDto: PersonDto): Int {
         deleteFailure?.let { throw it }
         deleteCount++
         return deletedRows
      }
   }

   @Test
   fun updateAndRemove_propagateDaoFailuresAsResults() = runTest {
      val dao = FakeDao()
      val repository = PersonRepository(dao)
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertSame(dao.updateFailure, repository.update(person).exceptionOrNull())
      assertSame(dao.deleteFailure, repository.remove(person).exceptionOrNull())

      dao.updateFailure = null
      dao.deleteFailure = null
      assertEquals(Unit, repository.update(person).getOrThrow())
      assertEquals(Unit, repository.remove(person).getOrThrow())
      assertTrue(dao.updateCount == 1)
      assertTrue(dao.deleteCount == 1)
   }

   @Test
   fun create_convertsRowIdToUnitAndMapsPerson() = runTest {
      val dao = FakeDao()
      val result: Result<Unit> = PersonRepository(dao).create(person)

      assertEquals(Unit, result.getOrThrow())
      assertEquals(person.id, dao.insertedPerson?.id)
      assertEquals(person.firstName, dao.insertedPerson?.firstName)
      assertEquals(person.lastName, dao.insertedPerson?.lastName)
   }

   @Test
   fun create_propagatesInsertFailureAsResult() = runTest {
      val dao = FakeDao()
      val failure = IllegalStateException("duplicate primary key")
      dao.insertFailure = failure

      assertSame(failure, PersonRepository(dao).create(person).exceptionOrNull())
   }

   @Test
   fun updateAndRemove_withNoMatchingRows_returnFailures() = runTest {
      val dao = FakeDao()
      dao.updatedRows = 0
      dao.deletedRows = 0
      val repository = PersonRepository(dao)

      val updateResult = repository.update(person)
      val removeResult = repository.remove(person)

      assertTrue(updateResult.isFailure)
      assertTrue(removeResult.isFailure)
      assertTrue(updateResult.exceptionOrNull() is IllegalStateException)
      assertTrue(removeResult.exceptionOrNull() is IllegalStateException)
      assertEquals("Update failed: Expected 1 row, got 0", updateResult.exceptionOrNull()?.message)
      assertEquals("Delete failed: Expected 1 row, got 0", removeResult.exceptionOrNull()?.message)
      assertEquals(1, dao.updateCount)
      assertEquals(1, dao.deleteCount)
   }

   @Test
   fun create_rethrowsCancellation() = runTest {
      val dao = FakeDao()
      val cancellation = CancellationException("insert cancelled")
      dao.insertFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao).create(person) }
   }

   @Test
   fun update_rethrowsCancellation() = runTest {
      val dao = FakeDao()
      val cancellation = CancellationException("update cancelled")
      dao.updateFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao).update(person) }
   }

   @Test
   fun remove_rethrowsCancellation() = runTest {
      val dao = FakeDao()
      val cancellation = CancellationException("delete cancelled")
      dao.deleteFailure = cancellation

      assertCancellation(cancellation) { PersonRepository(dao).remove(person) }
   }

   private suspend fun assertCancellation(
      expected: CancellationException,
      block: suspend () -> Result<Unit>,
   ) {
      try {
         block()
         fail("CancellationException must propagate")
      } catch (actual: CancellationException) {
         assertSame(expected, actual)
      }
   }
}
