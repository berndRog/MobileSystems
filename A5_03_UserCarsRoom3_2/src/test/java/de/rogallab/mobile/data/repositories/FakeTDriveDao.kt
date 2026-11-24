package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.readmodels.TDriveListRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTDriveDao : ITDriveDao {
   var insertFailure: Throwable? = null
   var updateFailure: Throwable? = null
   var deleteFailure: Throwable? = null
   var updatedRows = 1
   var deletedRows = 1
   var written: TDriveDto? = null
   override fun observeAllWithDriverAndCar(): Flow<List<TDriveListRow>> = flowOf(emptyList())
   override suspend fun findById(testDriveId: String): TDriveDto? = null
   override suspend fun count(): Int = 0
   override suspend fun insert(tDriveDto: TDriveDto): Long {
      insertFailure?.let { throw it }
      written = tDriveDto
      return 42L
   }
   override suspend fun insert(tDriveDtos: List<TDriveDto>) {
      tDriveDtos.forEach { insert(it) }
   }
   override suspend fun update(tDriveDto: TDriveDto): Int {
      updateFailure?.let { throw it }
      written = tDriveDto
      return updatedRows
   }
   override suspend fun delete(tDriveDto: TDriveDto): Int {
      deleteFailure?.let { throw it }
      written = tDriveDto
      return deletedRows
   }
}
