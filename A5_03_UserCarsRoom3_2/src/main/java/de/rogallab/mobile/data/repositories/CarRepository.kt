package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.mapping.toCar
import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkUpdated
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.resultOf

// Write side for the Car aggregate.
// Maps between the domain entity and the persistence model.
// No Flow here: reading for display is done by the read model.
class CarRepository(
   private val _dao: ICarDao,
) : ICarRepository {

   override suspend fun findById(id: String): Result<Car?> =
      resultOf(
         block = { _dao.findById(id)?.toCar() }
      )

   override suspend fun create(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowId = _dao.insert(car.toCarDto())
            checkInserted(rowId)
            Alog.d(TAG, "create: $car")
         }
      )

   override suspend fun update(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _dao.update(car.toCarDto())
            checkUpdated(rowsAffected)
            Alog.d(TAG, "update: $car")
         }
      )

   // ForeignKey.RESTRICT may fail here if other rows still reference this one.
   override suspend fun remove(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _dao.delete(car.toCarDto())
            checkDeleted(rowsAffected)
            Alog.d(TAG, "remove: $car")
         }
      )

   companion object { private const val TAG = "<-CarRepository" }
}
