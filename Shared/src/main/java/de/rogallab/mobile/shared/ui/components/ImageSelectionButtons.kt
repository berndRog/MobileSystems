package de.rogallab.mobile.shared.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.rogallab.mobile.shared.R
import de.rogallab.mobile.shared.domain.utilities.Alog

private const val TAG = "<-ImageSelectionBtns"

@Composable
fun ImageSelectionButtons(
   modifier: Modifier = Modifier,
   imagePath: String?,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit
) {
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { Alog.c(TAG, "Composition #${cCount.intValue++}") }


   Button(
      modifier = Modifier
         .fillMaxWidth(),
      onClick = onSelectPhoto
   ) {
      Text(stringResource(R.string.action_select_photo))
   }

   Button(
      modifier = Modifier
         .fillMaxWidth(),
      onClick = onTakePhoto
   ) {
      Text(stringResource(R.string.action_take_photo))
   }

   if (!imagePath.isNullOrBlank()) {
      OutlinedButton(
         modifier = Modifier
            .fillMaxWidth(),
         onClick = onRemovePhoto
      ) {
         Text(stringResource(R.string.action_remove_photo))
      }
   }

}