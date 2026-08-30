package de.rogallab.mobile.ui.tdrives.list.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.tdrives.list.TDrivesEffect
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel

@Composable
fun TDrivesAdapter(
   viewModel: TDrivesViewModel,
   contentPadding: PaddingValues,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, String) -> Unit,
   onNavigateTo: (String?) -> Unit,
) {
   val state by viewModel.stateFlow.collectAsStateWithLifecycle()
   val lazyListState = rememberLazyListState()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is TDrivesEffect.ShowMessage -> onMessage(effect.message)
         is TDrivesEffect.ShowError -> onError(effect.message)
         is TDrivesEffect.ConfirmRemove -> onConfirmRemove(effect.message, effect.actionLabel, effect.tDriveId)
         is TDrivesEffect.NavigateTo -> onNavigateTo(effect.tDriveId)
      }
   }

   TDrivesScreen(
      tDrivesUiState = state,
      lazyListState = lazyListState,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
