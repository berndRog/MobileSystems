package de.rogallab.mobile.domain.repositories

import de.rogallab.mobile.domain.entities.Person

interface IPersonRepository {
   suspend fun findById(id: String): Result<Person?>

   suspend fun create(person: Person): Result<Unit>
   suspend fun update(person: Person): Result<Unit>
   suspend fun remove(person: Person): Result<Unit>
}