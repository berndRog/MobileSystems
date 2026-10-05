package de.rogallab.mobile.shared.ui.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(
         8.dp,
         Alignment.CenterVertically
      ),
   ) {
      // Renders the button for selecting a photo from the gallery.
      Button(
         modifier = Modifier.fillMaxWidth(),
         onClick = onSelectPhoto,
         enabled = enabled,
      ) {
         Text(stringResource(R.string.action_select_photo))
      }
      // Renders the button for taking a photo with the camera.
      Button(
         modifier = Modifier.fillMaxWidth(),
         onClick = onTakePhoto,
         enabled = enabled,
      ) {
         Text(stringResource(R.string.action_take_photo))
      }

      // Renders the button for removing the current photo.
      if (!imagePath.isNullOrBlank()) {
         OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRemovePhoto,
            enabled = enabled,
         ) {
            Text(stringResource(R.string.action_remove_photo))
         }
      }
   }
}
