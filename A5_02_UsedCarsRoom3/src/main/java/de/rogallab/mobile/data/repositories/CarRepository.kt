package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.mapping.toCar
import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkUpdated
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.domain.utilities.resultOf
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class CarRepository(
   private val _carDao: ICarDao,
   private val _dispatcherIo: CoroutineDispatcher,
) : ICarRepository {
   override fun observeAll(): Flow<Result<List<Car>>> =
      _carDao.observeAll()
         .map { dtos -> Result.success(dtos.map(CarDto::toCar)) }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }
   override suspend fun findById(id: String): Result<Car?> =
      resultOf(
         block = { _carDao.findById(id)?.toCar() }
      )

   override suspend fun findByPersonId(personId: String): Result<List<Car>> =
      resultOf(
         block = { _carDao.findByPersonId(personId).map(CarDto::toCar) }
      )

   override suspend fun create(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowId = _carDao.insert(car.toCarDto())
            checkInserted(rowId)
            Alog.d(TAG, "create: $car")
         }
      )

   override suspend fun update(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _carDao.update(car.toCarDto())
            checkUpdated(rowsAffected)
            Alog.d(TAG, "update: $car")
         }
      )

   override suspend fun remove(car: Car): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _carDao.delete(car.toCarDto())
            checkDeleted(rowsAffected)
            Alog.d(TAG, "remove: $car")
         }
      )

   companion object { private const val TAG = "<-CarRepository" }
}
