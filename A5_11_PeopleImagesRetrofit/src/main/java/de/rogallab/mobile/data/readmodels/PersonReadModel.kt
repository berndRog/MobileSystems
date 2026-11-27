package de.rogallab.mobile.data.readmodels

import de.rogallab.mobile.data.mapping.toPersonListItem
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonListItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

// Retrofit's in-memory observation state is updated after successful HTTP writes.
class PersonReadModel(
   private val _repository: PersonRepository,
   private val _dispatcherIo: CoroutineDispatcher,
) : IPersonReadModel {
   override fun observeListItems(): Flow<Result<List<PersonListItem>>> =
      _repository.observeAll()
         .map { result -> result.map { people -> people.map { it.toPersonListItem() } } }
         .flowOn(_dispatcherIo)
         .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            emit(Result.failure(throwable))
         }
}
