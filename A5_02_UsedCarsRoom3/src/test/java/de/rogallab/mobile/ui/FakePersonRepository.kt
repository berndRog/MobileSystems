package de.rogallab.mobile.ui

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakePersonRepository(
   initialPeople: List<Person> = emptyList(),
) : IPersonRepository {

   private val _people = initialPeople.toMutableList()
   private val _observations = MutableSharedFlow<Result<List<Person>>>(replay = 1)

   var failCreate: Boolean = false
   var failRemove: Boolean = false

   var createCalls: Int = 0
      private set
   var updateCalls: Int = 0
      private set
   var removeCalls: Int = 0
      private set

   init {
      emitSnapshot()
   }

   val people: List<Person>
      get() = _people.toList()

   override fun observeAll(): Flow<Result<List<Person>>> = _observations

   override suspend fun findById(id: String): Result<Person?> =
      Result.success(
         _people.firstOrNull { person -> person.id == id }
      )

   override suspend fun create(person: Person): Result<Unit> {
      createCalls++
      if (failCreate) {
         return Result.failure(IllegalStateException("create failed"))
      }
      if (_people.any { currentPerson -> currentPerson.id == person.id }) {
         return Result.failure(IllegalStateException("duplicate"))
      }
      _people.add(person)
      emitSnapshot()
      return Result.success(Unit)
   }

   override suspend fun update(person: Person): Result<Unit> {
      updateCalls++
      val index = _people.indexOfFirst { currentPerson ->
         currentPerson.id == person.id
      }
      if (index < 0) {
         return Result.failure(IllegalStateException("missing"))
      }
      _people[index] = person
      emitSnapshot()
      return Result.success(Unit)
   }

   override suspend fun remove(person: Person): Result<Unit> {
      removeCalls++
      if (failRemove) {
         return Result.failure(IllegalStateException("remove failed"))
      }
      val removed = _people.removeAll { currentPerson ->
         currentPerson.id == person.id
      }
      if (!removed) {
         return Result.failure(IllegalStateException("missing"))
      }
      emitSnapshot()
      return Result.success(Unit)
   }

   // Emits a deliberately stale or reordered database snapshot for overlay tests.
   fun emitDatabaseSnapshot(people: List<Person>) {
      _observations.tryEmit(Result.success(people.toList()))
   }

   fun emitObservationFailure(
      throwable: Throwable = IllegalStateException("load failed"),
   ) {
      _observations.tryEmit(Result.failure(throwable))
   }

   private fun emitSnapshot() {
      _observations.tryEmit(Result.success(_people.toList()))
   }
}
