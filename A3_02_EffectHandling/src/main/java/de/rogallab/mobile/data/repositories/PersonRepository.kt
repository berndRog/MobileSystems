package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.local.IPersonDao
import de.rogallab.mobile.shared.data.local.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.collections.map
import kotlin.coroutines.cancellation.CancellationException

class PersonRepository(
   private val _personDao: IPersonDao  // ctor injection
) : IPersonRepository {

   private var _counterObserveAll = 0
   private var _counterFindById = 0

   override fun observeAll(): Flow<Result<List<Person>>> {
      //_counterObserveAll++
      //Alog.d(TAG,"observeAll(), counter=$_counterObserveAll")
      return _personDao.observeAll()
         .map { dtos ->
         //   if (_counterObserveAll == 1)
         //      throw RuntimeException("Test exception in observeAll()")
            Result.success(dtos.map(PersonDto::toPerson))
         }
         // Flow.catch handles upstream failures, but does not catch exceptions used for
         // flow cancellation. Therefore, CancellationException propagates normally.
         .catch { throwable ->
            emit(Result.failure(throwable))
         }
   }

   override suspend fun findById(id: String): Result<Person?> {
//      _counterFindById++
//      Alog.d(TAG,"findById(), counter=$_counterFindById")

      return try {
//         if(_counterFindById == 1)
//              Result.success(null)
//         else if(_counterFindById == 2)
//            throw RuntimeException("Test exception in findById()")
//         else
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
         //throw RuntimeException("Test exception in update()")
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
         throw RuntimeException("Test exception in remove()")
         Result.success(_personDao.delete(person.toPersonDto()))
      }
      catch (e: CancellationException) {
         throw e
      }
      catch (e: Throwable) {
         Result.failure(e)
      }

   companion object {
      private const val TAG = "<-PersonRepository"
   }

}

/*
 * Didaktik und Lernziele
 *
 * - Die Repository-Schnittstelle liefert dieselben Result-Werte wie in A3_03.
 * - CancellationException beendet Coroutines und wird nicht zum Ladefehler.
 */
