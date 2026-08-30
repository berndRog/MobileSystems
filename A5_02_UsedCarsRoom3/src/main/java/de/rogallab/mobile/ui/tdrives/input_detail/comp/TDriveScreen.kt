package de.rogallab.mobile.ui.tdrives.input_detail.comp

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
import de.rogallab.mobile.ui.composables.EditableScreenLayout
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveIntent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveUiState
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator

// Stateless editor used for both Create and Edit in this aspect.
//
// The nullable entity in the state separates initial loading from the actual
// form content. User-visible failures are emitted as UiText and displayed by
// the shared SnackbarHost instead of rendering a separate error page.
@Composable
fun TDriveScreen(
   tDriveUiState: TDriveUiState,
   validator: TDriveValidator,
   contentPadding: PaddingValues,
   onIntent: (TDriveIntent) -> Unit,
) {
   val tag = "<-TDriveScreen"
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(tag, "Composition #${compositionCount++}")
   }

   val tDrive = tDriveUiState.tDrive
   EditableScreenLayout(
      title = if (tDriveUiState.isNew) {
         R.string.test_drive_create_title
      }
      else {
         R.string.test_drive_edit_title
      },
      isLoading = tDriveUiState.isLoading,
      hasContent = tDrive != null,
      contentPadding = contentPadding,
      onCancel = { onIntent(TDriveIntent.Cancel) },
      onSave = { onIntent(TDriveIntent.Save) },
   ) {
      if (tDrive != null) {
         TDriveContent(
            tDrive = tDrive,
            startInput = tDriveUiState.startInput,
            people = tDriveUiState.people,
            cars = tDriveUiState.cars,
            validator = validator,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}
