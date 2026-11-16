package de.rogallab.mobile.ui.camerax.comp

import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import androidx.camera.compose.CameraXViewfinder

// Stateless: only renders state and reports events.
@Composable
fun CameraScreen(
   surfaceRequest: SurfaceRequest?,
   isRecording: Boolean,
   audioGranted: Boolean,
   onTakePhoto: () -> Unit,
   onToggleRecording: () -> Unit,
   modifier: Modifier = Modifier,
) {
   Box(modifier = modifier.fillMaxSize()) {

      // Viewfinder: shows the live preview
      surfaceRequest?.let {
         CameraXViewfinder(
            surfaceRequest = it,
            modifier = Modifier.fillMaxSize()
         )
      }

      if (isRecording) {
         Text(
            text = stringResource(R.string.camera_recording),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
               .align(Alignment.TopStart)
               .padding(16.dp),
         )
      }

      Column(
         modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(24.dp),
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
         if (!audioGranted) {
            Text(
               text = stringResource(R.string.camera_no_audio_hint),
               style = MaterialTheme.typography.bodySmall,
               color = Color.White,
            )
         }
         Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
               onClick = onTakePhoto,
               enabled = !isRecording
            ) {
               Text(stringResource(R.string.action_take_photo))
            }
            Button(
               onClick = onToggleRecording,
               colors =
                  if (isRecording)
                     ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error)
                  else
                     ButtonDefaults.buttonColors(),
            ) {
               Text(stringResource(
                  if (isRecording) R.string.action_stop_video else R.string.action_start_video))
            }
         }
      }
   }
}