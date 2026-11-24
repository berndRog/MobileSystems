package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.mapping.toTDriveListItem
import de.rogallab.mobile.domain.readmodels.ITDriveReadModel
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

// Read side for the TDrive table.
// Maps query results directly to projections, bypassing the domain entity TDrive.
class TDriveReadModel(
   private val _tDriveDao: ITDriveDao,
   private val _dispatcherIo: CoroutineDispatcher,
) : ITDriveReadModel {

   // Emits again whenever TDrive, Person or Car changes.
   override fun observeListItems(): Flow<Result<List<TDriveListItem>>> =
      _tDriveDao.observeAllWithDriverAndCar()
         .map { rows -> Result.success(rows.map(TDriveListRow::toTDriveListItem)) }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }
}
