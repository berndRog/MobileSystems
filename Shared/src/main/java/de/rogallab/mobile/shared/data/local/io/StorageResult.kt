package de.rogallab.mobile.shared.data.local.io

import kotlinx.coroutines.CancellationException

/**
 * Converts storage exceptions to Result while preserving coroutine cancellation.
 */
internal inline fun <T> storageResult(
   block: () -> T,
): Result<T> =
   try {
      Result.success(block())
   }
   catch (cancellationException: CancellationException) {
      throw cancellationException
   }
   catch (throwable: Throwable) {
      Result.failure(throwable)
   }
