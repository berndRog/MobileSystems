package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveIntent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveUiState
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator

@Composable
fun TDriveScreen(
   tDriveUiState: TDriveUiState,
   validator: TDriveValidator,
   onIntent: (TDriveIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-TDriveScreen"
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(tag, "Composition #${compositionCount++}")
   }

   val tDrive = tDriveUiState.tDrive ?: return

   Column(
      modifier = modifier
         .verticalScroll(rememberScrollState())
         .imePadding()
         .padding(
            start = 16.dp,
            top = 8.dp,
            end = 16.dp,
            bottom = 24.dp,
         ),
      verticalArrangement = Arrangement.spacedBy(16.dp),
   ) {
      TDriveContent(
         tDrive = tDrive,
         startInput = tDriveUiState.startInput,
         people = tDriveUiState.people,
         cars = tDriveUiState.cars,
         validator = validator,
         onIntent = onIntent,
         modifier = Modifier.fillMaxWidth(),
      )

      Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(
            40.dp,
            Alignment.CenterHorizontally,
         ),
      ) {
         OutlinedButton(onClick = { onIntent(TDriveIntent.Cancel) }) {
            Text(text = stringResource(R.string.action_cancel))
         }

         Button(onClick = { onIntent(TDriveIntent.Save) }) {
            Text(text = stringResource(R.string.action_save))
         }
      }
   }
}
