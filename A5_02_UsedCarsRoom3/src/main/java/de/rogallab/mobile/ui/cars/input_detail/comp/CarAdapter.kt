package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.people.create_detail.BackReason
import org.koin.compose.koinInject

@Composable
fun CarAdapter(
   viewModel: CarViewModel,
   contentPadding: PaddingValues,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: CarValidator = koinInject(),
) {
   val state by viewModel.stateFlow.collectAsStateWithLifecycle()
   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is CarEffect.ShowMessage -> onMessage(effect.message)
         is CarEffect.ShowError -> onError(effect.message)
         is CarEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }
   CarScreen(state, validator, contentPadding, viewModel::onIntent)
}
