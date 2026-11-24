package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.mapping.toCarListItem
import de.rogallab.mobile.data.mapping.toCarSelectItem
import de.rogallab.mobile.data.mapping.toSellerCarItem
import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.readmodels.SellerCarItem
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

// Read side for the Car table.
// Maps query results directly to projections, bypassing the domain entity Car.
class CarReadModel(
   private val _carDao: ICarDao,
   private val _dispatcherIo: CoroutineDispatcher,
) : ICarReadModel {

   // Emits again whenever Car or Person changes (both tables are part of the query).
   override fun observeListItems(): Flow<Result<List<CarListItem>>> =
      _carDao.observeAllWithSeller()
         .map { rows -> Result.success(rows.map(CarListRow::toCarListItem)) }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   override fun observeSelectItems(): Flow<Result<List<CarSelectItem>>> =
      _carDao.observeAll()
         .map { dtos -> Result.success(dtos.map(CarDto::toCarSelectItem)) }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

   // Loaded on demand when the bottom sheet is opened.
   override suspend fun findBySellerId(sellerId: String): Result<List<SellerCarItem>> =
      resultOf(
         block = { _carDao.findByPersonId(sellerId).map(CarDto::toSellerCarItem) }
      )
}
