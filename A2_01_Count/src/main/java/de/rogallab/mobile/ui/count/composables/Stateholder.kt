package de.rogallab.mobile.ui.count.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.rogallab.mobile.shared.domain.utilities.Alog

// Stateful Composable, holds state and passes it down to stateless composable CountScreen3

@Composable
fun Stateholder(
   initCount: Int,
   modifier: Modifier
) {
   val tag = "<-Stateholder"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Observable State,  state by delegate
   var count: Int by rememberSaveable { mutableIntStateOf(initCount) }

   // State change
   fun incrementCount() {
      count += 1
      Alog.d(tag, "onIncrementCount() $count")
   }

   CountScreen3(
      count = count,                           // State ↓
      onIncrementCount = { incrementCount() }, // Event ↑
      modifier = modifier
   )

}