package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonDto
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkUpdated
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.domain.utilities.resultOf
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonRepository(
   private val _personDao: IPersonDao,
) : IPersonRepository {
   override suspend fun findById(id: String): Result<Person?> =
      resultOf(
         block = { _personDao.findById(id)?.toPerson() }
      )

   override suspend fun create(person: Person): Result<Unit> =
      resultOf(
         block = {
            val rowId = _personDao.insert(person.toPersonDto())
            checkInserted(rowId)
            Alog.d(TAG, "create: $person")
         }
      )

   override suspend fun update(person: Person): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _personDao.update(person.toPersonDto())
            checkUpdated(rowsAffected)
            Alog.d(TAG, "update: $person")
         }
      )

   override suspend fun remove(person: Person): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _personDao.delete(person.toPersonDto())
            checkDeleted(rowsAffected)
            Alog.d(TAG, "remove: $person")
         }
      )

   companion object { private const val TAG = "<-PersonRepository" }
}
