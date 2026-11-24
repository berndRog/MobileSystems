package de.rogallab.mobile.domain.repositories

import de.rogallab.mobile.domain.entities.TDrive

// Write side for the TDrive aggregate.
interface ITDriveRepository {

   // Loads one aggregate for editing (TDriveViewModel) or deleting (TDriveUcDelete).
   suspend fun findById(id: String): Result<TDrive?>

   suspend fun create(tDrive: TDrive): Result<Unit>
   suspend fun update(tDrive: TDrive): Result<Unit>
   suspend fun remove(tDrive: TDrive): Result<Unit>
}
