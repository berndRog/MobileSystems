package de.rogallab.mobile.testing

import de.rogallab.mobile.domain.readmodels.ITDriveReadModel
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTDriveReadModel(items: List<TDriveListItem> = emptyList()) : ITDriveReadModel {
   val itemsFlow = MutableStateFlow(Result.success(items))

   override fun observeListItems(): Flow<Result<List<TDriveListItem>>> = itemsFlow
}
