package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import de.rogallab.mobile.data.mapping.toPeopleListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

class PersonReadModel(
   private val _personDao: IPersonDao,
   private val _dispatcherIo: CoroutineDispatcher,
) : IPersonReadModel {
   override fun observeListItems(): Flow<Result<List<PersonListItem>>> =
      _personDao.observeAll()  // Flow<List<PersonDto>>
         .map { dtos -> Result.success(dtos.map(PersonDto::toPeopleListItem)) }
         .flowOn(_dispatcherIo)
         .catch { t ->
            if (t is CancellationException) throw t
            emit(Result.failure(t))
         }
}