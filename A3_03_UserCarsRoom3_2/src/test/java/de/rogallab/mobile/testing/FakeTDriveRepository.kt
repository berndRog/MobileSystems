package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTDriveRepository(tDrives: List<TDrive> = emptyList()) : ITDriveRepository {
   val tDrivesFlow = MutableStateFlow(Result.success(tDrives))
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<TDrive>()
   override fun observeAll(): Flow<Result<List<TDrive>>> = tDrivesFlow
   override suspend fun findById(id: String): Result<TDrive?> =
      Result.success(tDrivesFlow.value.getOrDefault(emptyList()).firstOrNull { it.id == id })
   override suspend fun create(tDrive: TDrive): Result<Unit> = Result.success(Unit)
   override suspend fun update(tDrive: TDrive): Result<Unit> = Result.success(Unit)
   override suspend fun remove(tDrive: TDrive): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += tDrive
         tDrivesFlow.value = Result.success(tDrivesFlow.value.getOrDefault(emptyList()).filterNot { it.id == tDrive.id })
      }
      return removeResult
   }
}
