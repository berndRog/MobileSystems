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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.TextButton

// Stateless: only renders state and reports events.
@Composable
fun CameraScreen(
   surfaceRequest: SurfaceRequest?,
   isRecording: Boolean,
   audioGranted: Boolean,
   onTakePhoto: () -> Unit,
   onToggleRecording: () -> Unit,
   onOpenViewer: () -> Unit,
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
            IconButton(
               onClick = onTakePhoto,
               enabled = !isRecording
            ) {
               Icon(
                  imageVector = Icons.Filled.PhotoCamera,
                  contentDescription = stringResource(R.string.action_take_photo),
                  tint = Color.White
               )
            }
            IconButton(onClick = onToggleRecording) {
               Icon(
                  imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                  contentDescription = stringResource(
                     if (isRecording) R.string.action_stop_video else R.string.action_start_video),
                  tint = if (isRecording) Color.White else MaterialTheme.colorScheme.error,
               )
            }

            // onOpenViewer: () -> Unit
            TextButton(
               onClick = onOpenViewer,
               enabled = !isRecording,           // leaving the screen would unbind the camera
               modifier = Modifier
                  .align(Alignment.CenterVertically)
                  .padding(8.dp),
            ) {
               Text(stringResource(R.string.action_open_videos))
            }
         }
      }
   }
}