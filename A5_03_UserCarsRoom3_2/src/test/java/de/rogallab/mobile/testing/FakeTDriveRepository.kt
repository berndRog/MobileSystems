package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.repositories.ITDriveRepository

class FakeTDriveRepository(tDrives: List<TDrive> = emptyList()) : ITDriveRepository {
   private val entities = tDrives.toMutableList()
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<TDrive>()

   override suspend fun findById(id: String): Result<TDrive?> =
      Result.success(entities.firstOrNull { it.id == id })

   override suspend fun create(tDrive: TDrive): Result<Unit> = Result.success(Unit)
   override suspend fun update(tDrive: TDrive): Result<Unit> = Result.success(Unit)

   override suspend fun remove(tDrive: TDrive): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += tDrive
         entities.removeAll { it.id == tDrive.id }
      }
      return removeResult
   }
}
