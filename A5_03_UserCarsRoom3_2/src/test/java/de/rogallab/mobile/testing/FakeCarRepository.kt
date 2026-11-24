package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.repositories.ICarRepository

class FakeCarRepository(cars: List<Car> = emptyList()) : ICarRepository {
   private val entities = cars.toMutableList()
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<Car>()

   override suspend fun findById(id: String): Result<Car?> =
      Result.success(entities.firstOrNull { it.id == id })

   override suspend fun create(car: Car): Result<Unit> = Result.success(Unit)
   override suspend fun update(car: Car): Result<Unit> = Result.success(Unit)

   override suspend fun remove(car: Car): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += car
         entities.removeAll { it.id == car.id }
      }
      return removeResult
   }
}
