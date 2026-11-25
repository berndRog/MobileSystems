package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.mapping.toPersonListItem
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

// Read side for the Person table.
// Maps PersonDto directly to projections, bypassing the domain entity Person.
class PersonReadModel(
   private val _personDao: IPersonDao,
   private val _dispatcherIo: CoroutineDispatcher,
) : IPersonReadModel {

   override fun observeListItems(): Flow<Result<List<PersonListItem>>> =
      _personDao.observeAll()
         .map { dtos -> Result.success(dtos.map(PersonDto::toPersonListItem)) }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }

}
