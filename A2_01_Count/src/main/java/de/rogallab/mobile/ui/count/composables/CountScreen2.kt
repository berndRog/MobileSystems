package de.rogallab.mobile.ui.count.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun CountScreen2(
   initCount: Int,      // State ↓
   modifier: Modifier   // State ↓
) {
   val tag = "<-CountScreen2"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // using delegated property to get and set the value of the mutable state
   var count: Int by remember { mutableIntStateOf(initCount) }

   Column(
      modifier = modifier
   ) {

      Text(
         text = "$count",      // getter
         textAlign = TextAlign.Center,
         modifier = Modifier
            .border(border = BorderStroke(3.dp, Color.Gray))
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      )

      Button(
         onClick = {
            count++
            Alog.d(tag, "$count")
         },  // setter
         modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      ) {
         Text ( text = "Hochzählen" )
      }
   }
}