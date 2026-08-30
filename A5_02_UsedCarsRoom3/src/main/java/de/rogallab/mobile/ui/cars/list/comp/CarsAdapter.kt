package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.cars.list.CarsEffect
import de.rogallab.mobile.ui.cars.list.CarsViewModel

@Composable
fun CarsAdapter(
   viewModel: CarsViewModel,
   contentPadding: PaddingValues,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, String) -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val carListState by viewModel.stateFlow.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is CarsEffect.ShowMessage -> onMessage(effect.message)
         is CarsEffect.ShowError -> onError(effect.message)
         is CarsEffect.ConfirmRemove -> onConfirmRemove(
            effect.message,
            effect.actionLabel,
            effect.carId,
         )
         is CarsEffect.NavigateTo -> onNavigateTo(effect.carId)
      }
   }

   CarsScreen(
      carListState = carListState,
      lazyListState = lazyListState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
