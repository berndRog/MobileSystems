package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person

class FakePersonRepository : IPersonRepository {
   var findResult: Result<Person?> = Result.success(null)
   var createResult: Result<Unit> = Result.success(Unit)
   var updateResult: Result<Unit> = Result.success(Unit)
   var removeResult: Result<Unit> = Result.success(Unit)

   val created = mutableListOf<Person>()
   val updated = mutableListOf<Person>()
   val removed = mutableListOf<Person>()

   override suspend fun findById(id: String): Result<Person?> = findResult

   override suspend fun create(person: Person): Result<Unit> {
      if (createResult.isSuccess) created += person
      return createResult
   }

   override suspend fun update(person: Person): Result<Unit> {
      if (updateResult.isSuccess) updated += person
      return updateResult
   }

   override suspend fun remove(person: Person): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += person
      }
      return removeResult
   }
}
