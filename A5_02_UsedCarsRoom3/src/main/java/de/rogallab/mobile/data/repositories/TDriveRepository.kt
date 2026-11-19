package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.mapping.toTestDrive
import de.rogallab.mobile.data.mapping.toTestDriveDto
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkUpdated
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.domain.utilities.resultOf
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class TDriveRepository(private val _tDriveDao: ITDriveDao) : ITDriveRepository {
   override fun observeAll(): Flow<Result<List<TDrive>>> =
      _tDriveDao.observeAll()
         .map { dtos -> Result.success(dtos.map(TDriveDto::toTestDrive)) }
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }
   override suspend fun findById(id: String): Result<TDrive?> =
      resultOf(
         block = { _tDriveDao.findById(id)?.toTestDrive() }
      )

   override suspend fun create(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowId = _tDriveDao.insert(tDrive.toTestDriveDto())
            checkInserted(rowId)
            Alog.d(TAG, "create: $tDrive")
         }
      )

   override suspend fun update(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _tDriveDao.update(tDrive.toTestDriveDto())
            checkUpdated(rowsAffected)
            Alog.d(TAG, "update: $tDrive")
         }
      )

   override suspend fun remove(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _tDriveDao.delete(tDrive.toTestDriveDto())
            checkDeleted(rowsAffected)
            Alog.d(TAG, "remove: $tDrive")
         }
      )

   companion object { private const val TAG = "<-TDriveRepository" }
}
