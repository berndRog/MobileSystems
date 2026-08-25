package de.rogallab.mobile.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.Flow

@Composable
fun <T : Any> CollectEventsBy(
   events: Flow<T>,
   tag: String,
   minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
   onEvent: suspend (T) -> Unit,
) {

   val lifecycleOwner = LocalLifecycleOwner.current
   val lifecycle = lifecycleOwner.lifecycle

   // Uses the latest callback without restarting LaunchedEffect.
   val currentOnEvent by rememberUpdatedState(onEvent)

   // Collects events from the Flow when the lifecycle is at least in the specified state.
   LaunchedEffect(
      events,
      lifecycleOwner,
      minActiveState,
   ) {
      // Repeats the collection of events when the lifecycle is at least in the specified state.
      lifecycle.repeatOnLifecycle(minActiveState) {
         Alog.v(tag, "Start event collection / State: ${lifecycle.currentState}", )

         try {
            // Collects events from the Flow and invokes the current callback for each event.
            events.collect { event ->
               Alog.v(tag,"Event: ${event::class.simpleName}")
               currentOnEvent(event)
            }
         }
         finally {
            Alog.v(tag, "Stop event collection / State: ${lifecycle.currentState}", )
         }
      }
   }
}