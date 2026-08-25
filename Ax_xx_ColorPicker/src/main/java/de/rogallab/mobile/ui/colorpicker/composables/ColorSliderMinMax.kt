package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose

@Composable
fun ColorSliderMinMax(
    minText: String,          // State ↓
    minWeight: Float = 0.25f, // State ↓
    maxText: String,          // State ↓
    maxWeight: Float = 0.75f, // State ↓
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-ColorSliderMinMax","Composition #${nComp.intValue++}") }

   Row(
      modifier = Modifier.fillMaxWidth()
      //.border(border = BorderStroke(1.dp, Color.Gray))
   ) {
      Text(
         text = minText,
         modifier = Modifier.weight(minWeight),
         //.border(border = BorderStroke(1.dp, Color.Gray)),
         textAlign = TextAlign.End
      )
      Text(
         text = maxText,
         modifier = Modifier.weight(maxWeight),
         //.border(border = BorderStroke(1.dp, Color.Gray)),
         textAlign = TextAlign.End
      )
   }
}