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
      var changedRows = 0
      override fun observeAll(): Flow<List<PersonDto>> = flowOf(emptyList())
      override suspend fun findById(id: String): PersonDto? = null
      override suspend fun count(): Int = 0
      override suspend fun insert(personDto: PersonDto) = Unit
      override suspend fun insert(personDtos: List<PersonDto>) = Unit
      override suspend fun update(personDto: PersonDto): Int = changedRows
      override suspend fun delete(personDto: PersonDto): Int = changedRows
   }

   @Test
   fun updateAndRemove_reportMissingRowAsFailure() = runTest {
      val dao = FakeDao()
      val repository = PersonRepository(dao)
      assertTrue(repository.update(person).isFailure)
      assertTrue(repository.remove(person).isFailure)

      dao.changedRows = 1
      assertTrue(repository.update(person).isSuccess)
      assertTrue(repository.remove(person).isSuccess)
   }
}
