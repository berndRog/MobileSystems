package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose

@Composable
fun ColorBox(
   color: Color   // State ↓
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-ColorBox","Composition #${nComp.intValue++}") }

   Text(
      text = "",  // state
      modifier = Modifier
         .background(color)
         .border(border = BorderStroke(1.dp, Color.Gray))
         .padding(vertical = 32.dp)
         .fillMaxWidth()
   )
}