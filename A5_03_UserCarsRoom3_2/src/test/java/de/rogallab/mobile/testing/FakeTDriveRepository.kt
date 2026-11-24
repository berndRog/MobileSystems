package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.repositories.ITDriveRepository

class FakeTDriveRepository(tDrives: List<TDrive> = emptyList()) : ITDriveRepository {
   private val entities = tDrives.toMutableList()
   var removeResult: Result<Unit> = Result.success(Unit)
   val removed = mutableListOf<TDrive>()
   val created = mutableListOf<TDrive>()
   val updated = mutableListOf<TDrive>()
   val findCalls = mutableListOf<String>()
   var find: suspend (String) -> Result<TDrive?> = { id ->
      Result.success(entities.firstOrNull { it.id == id })
   }

   override suspend fun findById(id: String): Result<TDrive?> {
      findCalls += id
      return find(id)
   }

   override suspend fun create(tDrive: TDrive): Result<Unit> {
      created += tDrive
      return Result.success(Unit)
   }

   override suspend fun update(tDrive: TDrive): Result<Unit> {
      updated += tDrive
      return Result.success(Unit)
   }

   override suspend fun remove(tDrive: TDrive): Result<Unit> {
      if (removeResult.isSuccess) {
         removed += tDrive
         entities.removeAll { it.id == tDrive.id }
      }
      return removeResult
   }
}
