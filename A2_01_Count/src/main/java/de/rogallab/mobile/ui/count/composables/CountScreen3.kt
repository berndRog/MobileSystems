package de.rogallab.mobile.ui.count.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import de.rogallab.mobile.shared.domain.utilities.Alog

// Stateless Composable called by Stateholder or CountAdapter

@Composable
fun CountScreen3(
   count: Int,                                  // State ↓
   onIncrementCount: () -> Unit,                // Event ↑
   modifier: Modifier                           // Value
) {

   val tag = "<-CountScreen3"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   CountContent(
      count = count,                             // State ↓
      onIncrementCount = { onIncrementCount() }, // Event ↑
      modifier = modifier
   )

}