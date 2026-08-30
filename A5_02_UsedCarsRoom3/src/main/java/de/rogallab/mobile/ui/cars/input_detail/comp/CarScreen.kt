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

// Stateless editor used for both Create and Edit in this aspect.
//
// The nullable entity in the state separates initial loading from the actual
// form content. User-visible failures are emitted as UiText and displayed by
// the shared SnackbarHost instead of rendering a separate error page.
@Composable
fun CarScreen(
   carUiState: CarUiState,
   validator: CarValidator,
   contentPadding: PaddingValues,
   onIntent: (CarIntent) -> Unit,
) {
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(TAG, "Composition #${compositionCount++}")
   }

   val car = carUiState.car
   EditableScreenLayout(
      title = if (carUiState.isNew) {
         R.string.car_create_title
      }
      else {
         R.string.car_edit_title
      },
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
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}
