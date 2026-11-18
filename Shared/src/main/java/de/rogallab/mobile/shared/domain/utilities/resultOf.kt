package de.rogallab.mobile.shared.domain.utilities

import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T> resultOf(
   crossinline block: suspend () -> T,       // try { block }
   crossinline onFinally: () -> Unit = {},   // finally { onFinally }
): Result<T> =

   try {
      Result.success(block())
   }

   // Coroutine cancellation must be propagated.
   catch (e: CancellationException) {
      throw e
   }

   // Convert unexpected exceptions to Result.failure.
   catch (e: Exception) {
      Result.failure(e)
   }

   // Always executed, including return and exception.
   finally {
      onFinally()
   }