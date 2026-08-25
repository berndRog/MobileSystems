package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.colorpicker.colorToHex
import de.rogallab.mobile.ui.colorpicker.colorToString

@Composable
fun ColorLabel(
   color: Color,                 // State ↓
   modifier: Modifier = Modifier // State ↓
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-ColorLabel","Composition #${nComp.intValue++}") }

   Text(
      text =
         colorToString(color)
         +" "
         +colorToHex(color),
      textAlign = TextAlign.Center,
      modifier = modifier
         .padding(top = 8.dp)
         .fillMaxWidth()
   )
}