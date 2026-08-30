package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.cars.input_detail.CarIntent
import de.rogallab.mobile.ui.cars.input_detail.CarUiState
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.composables.EditableScreenLayout

private const val TAG = "<-CarScreen"

@Composable
fun CarScreen(
   carUiState: CarUiState,
   validator: CarValidator,
   contentPadding: PaddingValues,
   onSelectImages: () -> Unit,
   onTakePhoto: () -> Unit,
   onIntent: (CarIntent) -> Unit,
) {
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(TAG, "Composition #${compositionCount++}")
   }

   val car = carUiState.car

   EditableScreenLayout(
      title =
         if (carUiState.isNew) R.string.car_create_title
         else R.string.car_edit_title,
      isLoading = carUiState.isLoading,
      hasContent = car != null,
      contentPadding = contentPadding,
      onCancel = { onIntent(CarIntent.Cancel) },
      onSave = { onIntent(CarIntent.Save) },
   ) {
      if (car != null) {
         CarContent(
            car = car,
            registrationYearInput = carUiState.registrationYearInput,
            mileageInput = carUiState.mileageInput,
            priceInput = carUiState.priceInput,
            people = carUiState.people,
            validator = validator,
            onSelectImages = onSelectImages,
            onTakePhoto = onTakePhoto,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}
