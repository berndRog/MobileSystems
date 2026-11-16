package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// Selects one video and forwards its content URI.
@Composable
fun VideoPickerHandlerSingle(
   onVideoSelected: (Uri) -> Unit,
   content: @Composable (VideoPickerActions) -> Unit,
) {
   // Keep the latest callback available after recomposition.
   val currentOnVideoSelected by rememberUpdatedState(onVideoSelected)
   val videoRequest = remember {
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
   }
   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
   ) { uri ->
      // Closing the picker without a selection does not emit an event.
      uri?.let { currentOnVideoSelected(it) }
   }
   content(
      VideoPickerActions(
         selectVideo = { launcher.launch(videoRequest) }
      )
   )
}