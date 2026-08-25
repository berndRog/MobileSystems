package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose

@Composable
fun ColorSlider(
   label: String,                               // State ↓
   value: Float,                                // State ↓
   onValueChange: (Float) -> Unit,              // Event ↑
   valueRange: ClosedFloatingPointRange<Float>  // State ↓
      = 0f..1f,
   steps: Int = 255,                            // State ↓
   modifier: Modifier = Modifier                // State ↓
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-ColorSlider","Composition #${nComp.intValue++}") }

   Row(modifier = modifier.fillMaxWidth()) {
      Text(
         text = label,
         modifier = Modifier
            .weight(2f)
            .align(Alignment.CenterVertically),
         textAlign = TextAlign.Start
      )
      Slider(
         value = value,
         onValueChange = onValueChange,
         valueRange = valueRange,
         steps = steps,
         modifier = Modifier.weight(8f)
      )
   }
}