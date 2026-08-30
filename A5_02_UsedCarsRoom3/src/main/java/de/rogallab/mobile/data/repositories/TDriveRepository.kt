package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.mapping.toTestDrive
import de.rogallab.mobile.data.mapping.toTestDriveDto
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import kotlinx.coroutines.flow.Flow

class TDriveRepository(
   private val _tDriveDao: ITDriveDao,
) : ITDriveRepository {
   override fun observeAll(): Flow<Result<List<TDrive>>> =
      _tDriveDao.selectAll().asResult { tDriveDtos: List<TDriveDto> ->
         tDriveDtos.map { testDriveDto -> testDriveDto.toTestDrive() }
      }

   override suspend fun findById(id: String): Result<TDrive?> =
      runCatching { _tDriveDao.findById(id)?.toTestDrive() }

   override suspend fun create(tDrive: TDrive): Result<Unit> =
      runCatching { _tDriveDao.insert(tDrive.toTestDriveDto()) }

   override suspend fun update(tDrive: TDrive): Result<Unit> = runCatching {
      check(_tDriveDao.update(tDrive.toTestDriveDto()) == 1) {
         "TDrive ${tDrive.id} not found."
      }
   }

   override suspend fun remove(tDrive: TDrive): Result<Unit> = runCatching {
      check(_tDriveDao.delete(tDrive.toTestDriveDto()) == 1) {
         "TDrive ${tDrive.id} not found."
      }
   }
}
