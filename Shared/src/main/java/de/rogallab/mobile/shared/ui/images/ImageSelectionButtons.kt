package de.rogallab.mobile.shared.ui.images

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
   enabled: Boolean = true,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit,
) {
   val cCount = remember { mutableIntStateOf(0) }
   SideEffect { Alog.c(TAG, "Composition #${cCount.intValue++}") }

   // Renders the button for selecting a photo from the gallery.
   Button(
      modifier = modifier.fillMaxWidth(),
      onClick = onSelectPhoto,
      enabled = enabled,
   ) {
      Text(stringResource(R.string.action_select_photo))
   }
   // Renders the button for taking a photo with the camera.
   Button(
      modifier = modifier.fillMaxWidth(),
      onClick = onTakePhoto,
      enabled = enabled,
   ) {
      Text(stringResource(R.string.action_take_photo))
   }

   // Renders the button for removing the current photo.
   if (!imagePath.isNullOrBlank()) {
      OutlinedButton(
         modifier = modifier.fillMaxWidth(),
         onClick = onRemovePhoto,
         enabled = enabled,
      ) {
         Text(stringResource(R.string.action_remove_photo))
      }
   }
}
