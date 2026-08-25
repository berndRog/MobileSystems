package de.rogallab.mobile.ui.count.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog

// Stateless Composable called by CountScreen())

@Composable
fun CountContent(
   count: Int,                   // State ↓
   onIncrementCount: () -> Unit, // Event ↑
   modifier: Modifier            // Value
) {
   val tag = "<-CountContent"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   Column(modifier = modifier) {
      Text(
         text = "$count",      // getter
         textAlign = TextAlign.Center,
         modifier = Modifier
            //.border(border = BorderStroke(3.dp, Color.Gray))
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      )

      Button(
         onClick = { onIncrementCount() },  // setter
         modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
      ) {
         Text(text = "Hochzählen")
      }
   }
}