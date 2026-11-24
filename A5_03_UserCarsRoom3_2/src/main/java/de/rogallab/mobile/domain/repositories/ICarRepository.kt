package de.rogallab.mobile.domain.repositories

import de.rogallab.mobile.domain.entities.Car

// Write side for the Car aggregate.
// Loads a car for editing or deleting and persists changes.
interface ICarRepository {

   // Loads one aggregate for editing (CarViewModel) or deleting (CarUcDelete).
   suspend fun findById(id: String): Result<Car?>

   suspend fun create(car: Car): Result<Unit>
   suspend fun update(car: Car): Result<Unit>
   suspend fun remove(car: Car): Result<Unit>
}
