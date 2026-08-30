package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.cars.input_detail.CarEvent
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.common.UiText
import org.koin.compose.koinInject

@Composable
fun CarAdapter(
   viewModel: CarViewModel,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onSave: (Car, Boolean) -> Unit,
   onMessage: (UiText) -> Unit,
   validator: CarValidator = koinInject(),
) {
   val carUiState by viewModel.state.collectAsStateWithLifecycle()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { carEvent ->
         when (carEvent) {
            CarEvent.NavigateBack -> onBack()
            is CarEvent.RequestSave -> onSave(
               carEvent.car,
               carEvent.isNew,
            )
            is CarEvent.ShowSnackbar -> onMessage(carEvent.message)
         }
      }
   }

   CarScreen(
      carUiState = carUiState,
      validator = validator,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
