package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.mapping.toTestDrive
import de.rogallab.mobile.data.mapping.toTestDriveDto
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.repositories.ITDriveRepository
import de.rogallab.mobile.shared.data.local.checkDeleted
import de.rogallab.mobile.shared.data.local.checkInserted
import de.rogallab.mobile.shared.data.local.checkUpdated
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.resultOf

// Write side for the TDrive aggregate.
// Maps between the domain entity and the persistence model.
// No Flow here: reading for display is done by the read model.
class TDriveRepository(
   private val _dao: ITDriveDao,
) : ITDriveRepository {

   override suspend fun findById(id: String): Result<TDrive?> =
      resultOf(
         block = { _dao.findById(id)?.toTestDrive() }
      )

   override suspend fun create(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowId = _dao.insert(tDrive.toTestDriveDto())
            checkInserted(rowId)
            Alog.d(TAG, "create: $tDrive")
         }
      )

   override suspend fun update(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _dao.update(tDrive.toTestDriveDto())
            checkUpdated(rowsAffected)
            Alog.d(TAG, "update: $tDrive")
         }
      )

   // ForeignKey.RESTRICT may fail here if other rows still reference this one.
   override suspend fun remove(tDrive: TDrive): Result<Unit> =
      resultOf(
         block = {
            val rowsAffected = _dao.delete(tDrive.toTestDriveDto())
            checkDeleted(rowsAffected)
            Alog.d(TAG, "remove: $tDrive")
         }
      )

   companion object { private const val TAG = "<-TDriveRepository" }
}
