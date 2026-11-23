package de.rogallab.mobile.domain

import de.rogallab.mobile.domain.entities.TDrive
import kotlinx.coroutines.flow.Flow

interface ITDriveRepository {
   fun observeAll(): Flow<Result<List<TDrive>>>
   suspend fun findById(id: String): Result<TDrive?>
   suspend fun create(tDrive: TDrive): Result<Unit>
   suspend fun update(tDrive: TDrive): Result<Unit>
   suspend fun remove(tDrive: TDrive): Result<Unit>
}
