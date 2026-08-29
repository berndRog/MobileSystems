package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow

/**
 * Repository boundary between the Room-3 data model and the domain model.
 *
 * The repository exposes no Room type. It maps PersonDto to Person and wraps
 * database failures in Result so the ViewModels do not need persistence-
 * specific exception handling.
 */
class PersonRepository(
   private val _personDao: IPersonDao
) : IPersonRepository {

   override fun observeAll(): Flow<Result<List<Person>>> =
      _personDao.selectAll()
         .asResult { dtos: List<PersonDto> ->
            dtos.map(PersonDto::toPerson)
         }

   override suspend fun findById(id: String): Result<Person?> =
      runCatching {
         _personDao.findById(id)?.toPerson()
      }

   override suspend fun create(person: Person): Result<Unit> =
      runCatching {
         _personDao.insert(person.toPersonDto())
      }

   override suspend fun update(person: Person): Result<Unit> =
      runCatching {
         val changedRows = _personDao.update(person.toPersonDto())
         check(changedRows == 1) {
            "Person ${person.id} wurde nicht gefunden."
         }
      }

   override suspend fun remove(person: Person): Result<Unit> =
      runCatching {
         val changedRows = _personDao.delete(person.toPersonDto())
         check(changedRows == 1) {
            "Person ${person.id} wurde nicht gefunden."
         }
      }
}
