package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEvent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import org.koin.compose.koinInject

@Composable
fun TDriveAdapter(
   viewModel: TDriveViewModel,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onSave: (TDrive, Boolean) -> Unit,
   onMessage: (UiText) -> Unit,
   validator: TDriveValidator = koinInject(),
) {

   val tag = "<-TDriveAdapter"
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(tag, "Composition #${cCount.intValue++}") }


   val testDriveUiState by viewModel.state.collectAsStateWithLifecycle()

   LaunchedEffect(viewModel) {
      viewModel.events.collect { testDriveEvent ->
         when (testDriveEvent) {
            TDriveEvent.NavigateBack -> onBack()
            is TDriveEvent.RequestSave -> onSave(
               testDriveEvent.tDrive,
               testDriveEvent.isNew,
            )
            is TDriveEvent.ShowSnackbar ->
               onMessage(testDriveEvent.message)
         }
      }
   }

   TDriveScreen(
      tDriveUiState = testDriveUiState,
      validator = validator,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
