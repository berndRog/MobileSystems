package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import org.koin.compose.koinInject

@Composable
fun TDriveAdapter(
   viewModel: TDriveViewModel,
   contentPadding: PaddingValues,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: TDriveValidator = koinInject(),
) {
   val state by viewModel.stateFlow.collectAsStateWithLifecycle()
   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is TDriveEffect.ShowMessage -> onMessage(effect.message)
         is TDriveEffect.ShowError -> onError(effect.message)
         is TDriveEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }
   TDriveScreen(state, validator, contentPadding, viewModel::onIntent)
}
