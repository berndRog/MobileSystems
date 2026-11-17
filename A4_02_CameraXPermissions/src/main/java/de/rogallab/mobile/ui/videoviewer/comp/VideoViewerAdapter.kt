package de.rogallab.mobile.ui.videoviewer.comp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.images.VideoPickerHandlerSingle
import de.rogallab.mobile.shared.ui.images.VideoPlayer
import de.rogallab.mobile.ui.videoviewer.VideoViewerIntent
import de.rogallab.mobile.ui.videoviewer.VideoViewerViewModel

// Stateful: connects the ViewModel with the picker and the stateless player.
//
// Two states:
//   videoUri == null  -> selection: "Select video" / "Back to camera"
//   videoUri != null  -> player:    "Close" returns to the selection state
@Composable
fun VideoViewerAdapter(
   viewModel: VideoViewerViewModel,
   onNavigateBack: () -> Unit,
   modifier: Modifier = Modifier,
) {
   val uiState by viewModel.stateFlow.collectAsStateWithLifecycle()
   val videoUri = uiState.videoUri

   // While a video is shown, Back returns to the selection state first.
   // Without a video the handler is disabled and NavDisplay.onBack leaves the screen.
   BackHandler(enabled = videoUri != null) {
      viewModel.onIntent(VideoViewerIntent.ClearVideo)
   }

   VideoPickerHandlerSingle(
      onVideoSelected = { viewModel.onIntent(VideoViewerIntent.VideoSelected(it)) },
   ) { pickerActions ->

      if (videoUri == null) {
         // Selection state: pick a video or go back to the camera.
         Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Button(onClick = pickerActions.selectVideo) {
               Text(stringResource(R.string.action_select_video))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
               onClick = onNavigateBack
            ) {
               Text(stringResource(R.string.action_back_to_camera))
            }
         }
      } else {
         // Player state: leaving the composition releases the ExoPlayer (onDispose).
         VideoPlayer(
            uri = videoUri,
            onSelectOther = pickerActions.selectVideo,
            // Closing the player returns to the selection state, not to the camera.
            onClose = { viewModel.onIntent(VideoViewerIntent.ClearVideo) },
            modifier = modifier,
         )
      }
   }
}