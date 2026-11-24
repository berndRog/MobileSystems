package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.repositories.ICarRepository

class FakeCarRepository(cars: List<Car> = emptyList()) : ICarRepository {
   private val entities = cars.toMutableList()
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<Car>()
   val created = mutableListOf<Car>()
   val updated = mutableListOf<Car>()
   val findCalls = mutableListOf<String>()
   var find: suspend (String) -> Result<Car?> = { id ->
      Result.success(entities.firstOrNull { it.id == id })
   }

   override suspend fun findById(id: String): Result<Car?> {
      findCalls += id
      return find(id)
   }

   override suspend fun create(car: Car): Result<Unit> {
      created += car
      return Result.success(Unit)
   }

   override suspend fun update(car: Car): Result<Unit> {
      updated += car
      return Result.success(Unit)
   }

   override suspend fun remove(car: Car): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += car
         entities.removeAll { it.id == car.id }
      }
      return removeResult
   }
}
