package de.rogallab.mobile.ui.count.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog

// Stateful Composable,  State is created and used inside the Composable

@Composable
fun CountScreen1(
   initCount: Int,          // State ↓
   modifier: Modifier   // Value
) {
   val tag = "<-CountScreen1"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Observable State,  state by delegate
   val state: MutableState<Int> = remember { mutableIntStateOf(initCount) }

   Column(
      modifier = modifier
   ) {

      Text(
         text = state.value.toString(),  // state
         textAlign = TextAlign.Center,
         modifier = Modifier
            .border(border = BorderStroke(3.dp, Color.Gray))
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      )

      Button(
         onClick = {
            state.value++
            Alog.d(tag, "${state.value}")
         },  // setter
         modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      ) {
         Text ( text = "Hochzählen" )
      }
   }
}

