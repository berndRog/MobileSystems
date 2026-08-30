package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.Car
import kotlinx.coroutines.flow.Flow

interface ICarRepository {
   fun observeAll(): Flow<Result<List<Car>>>
   suspend fun findById(id: String): Result<Car?>
   suspend fun create(car: Car): Result<Unit>
   suspend fun update(car: Car): Result<Unit>
   suspend fun remove(car: Car): Result<Unit>
}
