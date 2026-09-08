package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose

@Composable
fun SelectColorScreen1(
   initialColor: Color = Color.Black,               // value
   modifier: Modifier                        // value
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-SelectColorScreen1","Composition #${nComp.intValue++}") }

   // StateHolder
   val stateColor: MutableState<Color> = remember {
      mutableStateOf(initialColor)
   }

   val red: Float = stateColor.value.red
   val green: Float = stateColor.value.green
   val blue: Float = stateColor.value.blue

   Column(
      modifier = modifier
         //.border(border = BorderStroke(1.dp, Color.Red))
   ) {
      ColorBox(
         color = stateColor.value,           // State ↓
      )
      ColorLabel(
         color = stateColor.value,           // State ↓
      )

      ColorSlider(
         label = "R(ot)",
         value = red,                        // State ↓
         onValueChange = { it ->             // Event ↑
            stateColor.value = Color(it, green, blue) }
      )
      ColorSlider(
         label = "G(rün)",
         value = green,                      // State ↓
         onValueChange = { it ->             // Event ↑
            stateColor.value = Color(red, it, blue) }
      )
      ColorSlider(
         label = "B(lau)",
         value = blue,                       // State ↓
         onValueChange = { it ->             // Event ↑
            stateColor.value = Color(red, green, it) }
      )

      ColorSliderMinMax(
         minText= "Min",
         maxText = "Max",
      )
   }
}







