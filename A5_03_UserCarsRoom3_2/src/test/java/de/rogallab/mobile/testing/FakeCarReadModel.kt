package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.readmodels.SellerCarItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeCarReadModel(items: List<CarListItem> = emptyList()) : ICarReadModel {
   val itemsFlow = MutableStateFlow(Result.success(items))

   override fun observeListItems(): Flow<Result<List<CarListItem>>> = itemsFlow
   override fun observeSelectItems(): Flow<Result<List<CarSelectItem>>> =
      flowOf(Result.success(emptyList()))

   override suspend fun findBySellerId(sellerId: String): Result<List<SellerCarItem>> =
      Result.success(emptyList())
}
