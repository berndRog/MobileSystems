package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.mapping.toTestDrive
import de.rogallab.mobile.data.mapping.toTestDriveDto
import de.rogallab.mobile.data.remote.ITDriveWebservice
import de.rogallab.mobile.data.remote.dtos.TDriveDto
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class TDriveRepository(
   private val _webservice: ITDriveWebservice,
   private val _networkExceptionMapper: NetworkExceptionMapper,
) : ITDriveRepository {
   private val _state = MutableStateFlow<Result<List<TDrive>>>(Result.success(emptyList()))

   override fun observeAll(): Flow<Result<List<TDrive>>> = flow {
      refresh()
      emitAll(_state)
   }

   override suspend fun findById(id: String): Result<TDrive?> =
      try {
         val tDrive = _webservice.getById(id).toTestDrive()
         upsert(tDrive)
         Result.success(tDrive)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (exception: HttpException) {
         if (exception.code() == 404) Result.success(null)
         else Result.failure(_networkExceptionMapper.map(exception))
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   override suspend fun create(tDrive: TDrive): Result<Unit> = write {
      upsert(_webservice.create(tDrive.toTestDriveDto()).toTestDrive())
   }

   override suspend fun update(tDrive: TDrive): Result<Unit> = write {
      upsert(_webservice.update(tDrive.id, tDrive.toTestDriveDto()).toTestDrive())
   }

   override suspend fun remove(tDrive: TDrive): Result<Unit> = write {
      val response = _webservice.delete(tDrive.id)
      if (!response.isSuccessful) throw HttpException(response)
      _state.value = Result.success(
         _state.value.getOrDefault(emptyList()).filterNot { it.id == tDrive.id }
      )
   }

   private suspend fun refresh() {
      _state.value = try {
         Result.success(sorted(_webservice.getAll().map(TDriveDto::toTestDrive)))
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }
   }

   private fun upsert(tDrive: TDrive) {
      val values = _state.value.getOrDefault(emptyList()).filterNot { it.id == tDrive.id } + tDrive
      _state.value = Result.success(sorted(values))
   }

   private fun sorted(values: List<TDrive>) = values.sortedWith(
      compareBy<TDrive> { it.isCompleted }.thenBy { it.start }
   )

   private suspend fun write(block: suspend () -> Unit): Result<Unit> =
      try {
         block()
         Result.success(Unit)
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }
}
