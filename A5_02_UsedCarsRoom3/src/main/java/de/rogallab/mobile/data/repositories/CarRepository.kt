package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.mapping.toCar
import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.Car
import kotlinx.coroutines.flow.Flow

class CarRepository(
   private val _carDao: ICarDao,
) : ICarRepository {
   override fun observeAll(): Flow<Result<List<Car>>> =
      _carDao.selectAll().asResult { carDtos: List<CarDto> ->
         carDtos.map { carDto -> carDto.toCar() }
      }

   override suspend fun findById(id: String): Result<Car?> =
      runCatching { _carDao.findById(id)?.toCar() }

   override suspend fun create(car: Car): Result<Unit> =
      runCatching { _carDao.insert(car.toCarDto()) }

   override suspend fun update(car: Car): Result<Unit> = runCatching {
      check(_carDao.update(car.toCarDto()) == 1) { "Car ${car.id} not found." }
   }

   override suspend fun remove(car: Car): Result<Unit> = runCatching {
      check(_carDao.delete(car.toCarDto()) == 1) { "Car ${car.id} not found." }
   }
}
