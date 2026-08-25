package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.shared.data.local.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.IPersonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.collections.map
import kotlin.coroutines.cancellation.CancellationException

class PersonRepository(
   private val _personDao: IPersonDao  // ctor injection
) : IPersonRepository {

   override fun observeAll(): Flow<Result<List<Person>>> =
      _personDao.observeAll()
         .map { dtos ->
            Result.success(dtos.map(PersonDto::toPerson))
         }
         // Flow.catch handles upstream failures, but does not catch exceptions used for
         // flow cancellation. Therefore, CancellationException propagates normally.
         .catch { throwable ->
            emit(Result.failure(throwable))
         }

   override suspend fun findById(id: String): Result<Person?> =
      try {
         Result.success(_personDao.findById(id)?.toPerson())
      }
      // don't handle CancellationException, let it propagate to the caller
      // viewModelScope will catch it and cancel the coroutine
      catch (e: CancellationException) {
         throw e
      }
      // handle other exceptions and return a failure result
      catch (e: Throwable) {
         Result.failure(e)
      }

   override suspend fun create(person: Person): Result<Unit> =
      try {
         Result.success(_personDao.insert(person.toPersonDto()))
      }
      catch (e: CancellationException) {
         throw e
      }
      catch (e: Throwable) {
         Result.failure(e)
      }

   override suspend fun update(person: Person): Result<Unit> =
      try {
         Result.success(_personDao.update(person.toPersonDto()))
      }
      catch (e: CancellationException) {
         throw e
      }
      catch (e: Throwable) {
         Result.failure(e)
      }

   override suspend fun remove(person: Person): Result<Unit> =
      try {
         Result.success(_personDao.delete(person.toPersonDto()))
      }
      catch (e: CancellationException) {
         throw e
      }
      catch (e: Throwable) {
         Result.failure(e)
      }

}