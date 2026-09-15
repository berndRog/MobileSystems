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

class PersonRepository(
   private val _personDao: IPersonDao,
) : IPersonRepository {

   override fun observeAll(): Flow<Result<List<Person>>> =
      _personDao.observeAll()
         .map { dtos: List<PersonDto> ->
            Result.success(dtos.map(PersonDto::toPerson))
         }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override suspend fun findById(id: String): Result<Person?> =
      resultOf {
         _personDao.findById(id)?.toPerson()
      }

   override suspend fun create(person: Person): Result<Unit> =
      resultOf {
         _personDao.insert(person.toPersonDto())
      }

   override suspend fun update(person: Person): Result<Unit> =
      resultOf {
         checkRows(_personDao.update(person.toPersonDto()), person)
      }

   override suspend fun remove(person: Person): Result<Unit> =
      resultOf {
         checkRows(_personDao.delete(person.toPersonDto()), person)
      }

   private fun checkRows(changedRows: Int, person: Person): Result<Unit> =
      if (changedRows == 1) {
         Result.success(Unit)
      } else {
         Result.failure(
            IllegalArgumentException("Person ${person.id} was not found.")
         )
      }

   private suspend fun <T> resultOf(
      block: suspend () -> T,
   ): Result<T> =
      try {
         Result.success(block())
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   companion object {
      private const val TAG = "<-PersonRepository"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonRepository implementiert dieselbe Domain-Schnittstelle wie in A4_01.
 *   Für ViewModels ändert sich deshalb beim Wechsel zu Room 3 nichts.
 *
 * - Nur innerhalb des Repository werden Person und PersonDto ineinander
 *   überführt. Datenbankfehler werden als Result an die darüberliegende Schicht
 *   weitergereicht.
 *
 * - CancellationException wird nicht in Result.failure umgewandelt, damit die
 *   strukturierte Coroutine-Cancellation erhalten bleibt.
 */

/*
class PersonRepository(
private val _personDao: IPersonDao,
) : IPersonRepository {

   override fun observeAll(): Flow<Result<List<Person>>> =
      _personDao.observeAll()
         .map { dtos: List<PersonDto> ->
            Result.success(dtos.map(PersonDto::toPerson))
         }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override suspend fun findById(id: String): Result<Person?> =
//      try {
//         Alog.d(TAG, "findById: $id")
//         Result.success(_personDao.findById(id)?.toPerson())
//      }
//      catch (exception: CancellationException) {
//         throw exception
//      }
//      catch (throwable: Throwable) {
//         Result.failure(throwable)
//      }
      resultOf {
         _personDao.findById(id)?.toPerson()
      }

   override suspend fun create(person: Person): Result<Unit> =
//      try {
//         _personDao.insert(person.toPersonDto())
//         Alog.d(TAG, "create: $person")
//         Result.success(Unit)
//      }
//      catch (exception: CancellationException) {
//         throw exception
//      }
//      catch (throwable: Throwable) {
//         Result.failure(throwable)
//      }
      resultOf {
         _personDao.insert(person.toPersonDto())
      }

   override suspend fun update(person: Person): Result<Unit> =
//      try {
//         Alog.d(TAG, "update: $person")
//         val changedRows = _personDao.update(person.toPersonDto())
//         if (changedRows == 1)
//            Result.success(Unit)
//         else
//            Result.failure(IllegalArgumentException("Person ${person.id} was not found."))
//
//      } catch (exception: CancellationException) {
//         throw exception
//      } catch (throwable: Throwable) {
//         Result.failure(throwable)
//      }

      resultOf {
         checkRows(_personDao.update(person.toPersonDto()), person)
      }

   override suspend fun remove(person: Person): Result<Unit> =
//      try {
//         val changedRows = _personDao.delete(person.toPersonDto())
//         if (changedRows == 1)
//            Result.success(Unit)
//         else
//            Result.failure(IllegalArgumentException("Person ${person.id} was not found."))
//      } catch (exception: CancellationException) {
//         throw exception
//      } catch (throwable: Throwable) {
//         Result.failure(throwable)
//      }

      resultOf {
         checkRows(_personDao.delete(person.toPersonDto()), person)
      }

   private fun checkRows(changedRows: Int, person: Person): Result<Unit> =
      if (changedRows == 1) {
         Result.success(Unit)
      } else {
         Result.failure(
            IllegalArgumentException("Person ${person.id} was not found.")
         )
      }

   private suspend fun <T> resultOf(
      block: suspend () -> T,
   ): Result<T> =
      try {
         Result.success(block())
      } catch (exception: CancellationException) {
         throw exception
      } catch (throwable: Throwable) {
         Result.failure(throwable)
      }

   companion object {
      private const val TAG = "<-PersonRepository"
   }
}

*/