package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.mapping.toCar
import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class CarRepository(private val _carDao: ICarDao) : ICarRepository {
   override fun observeAll(): Flow<Result<List<Car>>> =
      _carDao.observeAll()
         .map { dtos -> Result.success(dtos.map(CarDto::toCar)) }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }
   override suspend fun findById(id: String): Result<Car?> = resultOf { _carDao.findById(id)?.toCar() }
   override suspend fun create(car: Car): Result<Unit> = write("create", car) { _carDao.insert(car.toCarDto()) }
   override suspend fun update(car: Car): Result<Unit> = write("update", car) {
      check(_carDao.update(car.toCarDto()) == 1) { "Car ${car.id} was not found." }
   }
   override suspend fun remove(car: Car): Result<Unit> = write("remove", car) {
      check(_carDao.delete(car.toCarDto()) == 1) { "Car ${car.id} was not found." }
   }
   private suspend fun write(operation: String, car: Car, block: suspend () -> Unit): Result<Unit> =
      try { block(); Alog.d(TAG, "$operation: $car"); Result.success(Unit) }
      catch (exception: CancellationException) { throw exception }
      catch (throwable: Throwable) { Result.failure(throwable) }
   private suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
      try { Result.success(block()) }
      catch (exception: CancellationException) { throw exception }
      catch (throwable: Throwable) { Result.failure(throwable) }
   companion object { private const val TAG = "<-CarRepository" }
}
