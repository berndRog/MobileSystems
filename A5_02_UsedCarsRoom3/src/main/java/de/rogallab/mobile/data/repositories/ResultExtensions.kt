package de.rogallab.mobile.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

inline fun <T, R> Flow<T>.asResult(
   crossinline transform: suspend (T) -> R
): Flow<Result<R>> =
   map { value -> Result.success(transform(value)) }
      .catch { throwable -> emit(Result.failure(throwable)) }
