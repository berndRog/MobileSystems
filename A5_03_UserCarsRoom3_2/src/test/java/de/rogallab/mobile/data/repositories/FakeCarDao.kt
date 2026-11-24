package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.readmodels.CarListRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeCarDao : ICarDao {
   var insertFailure: Throwable? = null
   var updateFailure: Throwable? = null
   var deleteFailure: Throwable? = null
   var updatedRows = 1
   var deletedRows = 1
   var written: CarDto? = null
   override fun observeAllWithSeller(): Flow<List<CarListRow>> = flowOf(emptyList())
   override fun observeAll(): Flow<List<CarDto>> = flowOf(emptyList())
   override suspend fun findById(carId: String): CarDto? = null
   override suspend fun findByPersonId(personId: String): List<CarDto> = emptyList()
   override suspend fun count(): Int = 0
   override suspend fun insert(carDto: CarDto): Long {
      insertFailure?.let { throw it }
      written = carDto
      return 42L
   }
   override suspend fun insert(carDtos: List<CarDto>) {
      carDtos.forEach { insert(it) }
   }
   override suspend fun update(carDto: CarDto): Int {
      updateFailure?.let { throw it }
      written = carDto
      return updatedRows
   }
   override suspend fun delete(carDto: CarDto): Int {
      deleteFailure?.let { throw it }
      written = carDto
      return deletedRows
   }
}
