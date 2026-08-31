package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.domain.ResultData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> handleApiRequest(
   // api call is function type
   apiCall: suspend () -> T,
): Flow<ResultData<T>> = flow {

   try {
      emit(ResultData.Loading)
      // make the api call
      val data: T = apiCall()
      emit(ResultData.Success(data))
   } catch (e: Exception) {
      emit(ResultData.Error(e))
   }
}
