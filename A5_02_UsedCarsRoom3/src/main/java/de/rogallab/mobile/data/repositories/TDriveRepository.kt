package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.data.mapping.toTestDrive
import de.rogallab.mobile.data.mapping.toTestDriveDto
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
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
   override suspend fun findById(id: String): Result<TDrive?> = resultOf { _tDriveDao.findById(id)?.toTestDrive() }
   override suspend fun create(tDrive: TDrive): Result<Unit> = write("create", tDrive) { _tDriveDao.insert(tDrive.toTestDriveDto()) }
   override suspend fun update(tDrive: TDrive): Result<Unit> = write("update", tDrive) {
      check(_tDriveDao.update(tDrive.toTestDriveDto()) == 1) { "TDrive ${tDrive.id} was not found." }
   }
   override suspend fun remove(tDrive: TDrive): Result<Unit> = write("remove", tDrive) {
      check(_tDriveDao.delete(tDrive.toTestDriveDto()) == 1) { "TDrive ${tDrive.id} was not found." }
   }
   private suspend fun write(operation: String, tDrive: TDrive, block: suspend () -> Unit): Result<Unit> =
      try { block(); Alog.d(TAG, "$operation: $tDrive"); Result.success(Unit) }
      catch (exception: CancellationException) { throw exception }
      catch (throwable: Throwable) { Result.failure(throwable) }
   private suspend fun <T> resultOf(block: suspend () -> T): Result<T> =
      try { Result.success(block()) }
      catch (exception: CancellationException) { throw exception }
      catch (throwable: Throwable) { Result.failure(throwable) }
   companion object { private const val TAG = "<-TDriveRepository" }
}
