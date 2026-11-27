package de.rogallab.mobile.testing

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

// Counts real collections and their overlap, including delayed upstream cleanup.
class ObservationProbe<T>(items: List<T> = emptyList()) {
   var upstream: Flow<Result<List<T>>> = MutableStateFlow(Result.success(items))
   var cleanupDelay = 0L
   var collections = 0
      private set
   var active = 0
      private set
   var maxActive = 0
      private set
   var completions = 0
      private set

   fun observe(): Flow<Result<List<T>>> = flow {
      collections++
      active++
      maxActive = maxOf(maxActive, active)
      try {
         emitAll(upstream)
      } finally {
         try {
            withContext(NonCancellable) { delay(cleanupDelay) }
         } finally {
            active--
            completions++
         }
      }
   }
}
