package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class PersonRepository(private val _personDao: IPersonDao) : IPersonRepository {
   override fun observeAll(): Flow<Result<List<Person>>> =
      _personDao.observeAll()
         .map { dtos -> Result.success(dtos.map(PersonDto::toPerson)) }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override suspend fun findById(id: String): Result<Person?> =
      resultOf { _personDao.findById(id)?.toPerson() }

   override suspend fun create(person: Person): Result<Unit> =
      write("create", person) { _personDao.insert(person.toPersonDto()) }

   override suspend fun update(person: Person): Result<Unit> =
      write("update", person) {
         check(_personDao.update(person.toPersonDto()) == 1) { "Person ${person.id} was not found." }
      }

   override suspend fun remove(person: Person): Result<Unit> =
      write("remove", person) {
         check(_personDao.delete(person.toPersonDto()) == 1) { "Person ${person.id} was not found." }
      }

   private suspend fun write(operation: String, person: Person, block: suspend () -> Unit): Result<Unit> =
      try {
         block(); Alog.d(TAG, "$operation: $person"); Result.success(Unit)
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   private suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
      try { Result.success(block()) }
      catch (exception: CancellationException) { throw exception }
      catch (throwable: Throwable) { Result.failure(throwable) }

   companion object { private const val TAG = "<-PersonRepository" }
}
