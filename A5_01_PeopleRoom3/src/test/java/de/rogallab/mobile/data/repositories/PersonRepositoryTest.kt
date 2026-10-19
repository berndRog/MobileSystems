package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonRepositoryTest {
   private val person = Person(firstName = "Ada", lastName = "Lovelace", id = "p1")

   private class FakeDao : IPersonDao {
      var updateFailure: Throwable? = null
      var deleteFailure: Throwable? = null
      var updateCount = 0
      var deleteCount = 0
      override fun observeAll(): Flow<List<PersonDto>> = flowOf(emptyList())
      override suspend fun findById(id: String): PersonDto? = null
      override suspend fun count(): Int = 0
      override suspend fun insert(personDto: PersonDto) = Unit
      override suspend fun insert(personDtos: List<PersonDto>) = Unit
      override suspend fun update(personDto: PersonDto) {
         updateFailure?.let { throw it }
         updateCount++
      }
      override suspend fun delete(personDto: PersonDto) {
         deleteFailure?.let { throw it }
         deleteCount++
      }
   }

   @Test
   fun updateAndRemove_propagateDaoFailuresAsResults() = runTest {
      val dao = FakeDao()
      val repository = PersonRepository(dao)
      dao.updateFailure = IllegalStateException("update failed")
      dao.deleteFailure = IllegalStateException("delete failed")
      assertTrue(repository.update(person).isFailure)
      assertTrue(repository.remove(person).isFailure)

      dao.updateFailure = null
      dao.deleteFailure = null
      assertTrue(repository.update(person).isSuccess)
      assertTrue(repository.remove(person).isSuccess)
      assertTrue(dao.updateCount == 1)
      assertTrue(dao.deleteCount == 1)
   }
}
