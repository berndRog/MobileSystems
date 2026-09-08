package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.colorpicker.ColorPickerViewModel

// Color from by StateFlow from ViewModel
@Composable
fun SelectColorScreen2b(
   viewModel: ColorPickerViewModel = viewModel(),
   modifier: Modifier
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-SelectColorScreen2b","Composition #${nComp.intValue++}") }

   val color: Color by viewModel.colorUiStateFlow.collectAsStateWithLifecycle()

   val red: Float = color.red                // State ↓
   val green: Float = color.green            // State ↓
   val blue: Float = color.blue              // State ↓

   Column(
      modifier = modifier
         //.border(border = BorderStroke(1.dp, Color.Red))
   ) {
      ColorBox(
         color = color,                      // State ↓
      )
      ColorLabel(
         color = color,                      // State ↓
      )

      ColorSlider(
         label = "R(ot)",
         value = red,                        // State ↓
         onValueChange = { it ->             // Event ↑
            viewModel.onColorChange( Color(it, green, blue)) }
      )
      ColorSlider(
         label = "G(rün)",
         value = green,                      // State ↓
         onValueChange = { it ->             // Event ↑
            viewModel.onColorChange( Color(red, it, blue)) }
      )
      ColorSlider(
         label = "B(lau)",
         value = blue,                       // State ↓
         onValueChange = { it ->             // Event ↑
            viewModel.onColorChange( Color(red, green, it)) }
      )

      ColorSliderMinMax(
         minText= "Min",
         maxText = "Max",
      )
   }
}







