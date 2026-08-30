package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.Car
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCarRepository(cars: List<Car> = emptyList()) : ICarRepository {
   val carsFlow = MutableStateFlow(Result.success(cars))
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<Car>()
   override fun observeAll(): Flow<Result<List<Car>>> = carsFlow
   override suspend fun findById(id: String): Result<Car?> =
      Result.success(carsFlow.value.getOrDefault(emptyList()).firstOrNull { it.id == id })
   override suspend fun create(car: Car): Result<Unit> = Result.success(Unit)
   override suspend fun update(car: Car): Result<Unit> = Result.success(Unit)
   override suspend fun remove(car: Car): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += car
         carsFlow.value = Result.success(carsFlow.value.getOrDefault(emptyList()).filterNot { it.id == car.id })
      }
      return removeResult
   }
}
