package de.rogallab.mobile.shared.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.StateFlow

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycleLogs(
   tag: String,
): T {

   // Get the current lifecycle owner
   val lifecycleOwner = LocalLifecycleOwner.current
   // Get the current lifecycle
   val lifecycle = lifecycleOwner.lifecycle
   // Get the current ViewModelStoreOwner
   val viewModelStoreOwner = LocalViewModelStoreOwner.current

   // Log lifecycle events and state changes
   DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
         Alog.v(tag,"Lifecycle Event: $event / State: ${lifecycle.currentState}", )
      }
      lifecycle.addObserver(observer)

      onDispose {  lifecycle.removeObserver(observer) }
   }

   // Log recomposition and lifecycle state
   SideEffect {
      Alog.v(tag, "Recomposition ViewModelStoreOwner: $viewModelStoreOwner / " +
            "ViewModelStore available: ${viewModelStoreOwner != null}")
   }

   // Collect the StateFlow as state with lifecycle awareness
   val value by this.collectAsStateWithLifecycle(
      lifecycle = lifecycle,
      minActiveState = Lifecycle.State.STARTED,
   )
   return value
}