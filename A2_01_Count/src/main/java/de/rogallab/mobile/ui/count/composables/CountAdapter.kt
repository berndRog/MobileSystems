package de.rogallab.mobile.ui.count.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.count.CountViewModel

// Stateful Composable

@Composable  // Stateful
fun CountAdapter(
   viewModel: CountViewModel = viewModel(),
   modifier: Modifier
) {
   val tag = "<-CountAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Observe state changes of count, via StateFlow
   val count: Int
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   CountScreen3(
      count = count,                                     // State ↓
      onIncrementCount = { viewModel.incrementCount() }, // Event ↑
      modifier = modifier
   )


}
