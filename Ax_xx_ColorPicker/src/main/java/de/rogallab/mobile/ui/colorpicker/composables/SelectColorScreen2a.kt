package de.rogallab.mobile.ui.colorpicker.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.colorpicker.ColorPickerViewModel

// with StateFlow from ViewModel as State<Color>
@Composable
fun SelectColorScreen2a(
   viewModel: ColorPickerViewModel = viewModel(),
   modifier: Modifier
) {
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose("<-SelectColorScreen2a","Composition #${nComp.intValue++}") }

   // State<Color> = StateFlow from ViewModel
   val stateColor: State<Color> =  viewModel.colorUiStateFlow.collectAsStateWithLifecycle()

   val red: Float = stateColor.value.red            // State ↓
   val green: Float = stateColor.value.green        // State ↓
   val blue: Float = stateColor.value.blue          // State ↓

   AppLogger.debug("<-SliderScreen2", "Composition")

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







