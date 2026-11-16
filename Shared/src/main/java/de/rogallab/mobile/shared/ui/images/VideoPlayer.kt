package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import de.rogallab.mobile.shared.R

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
   uri: Uri,
   onClose: () -> Unit,
   onSelectOther: () -> Unit,
   modifier: Modifier = Modifier,
) {
   val context = LocalContext.current

   // The player belongs to the composition: created per uri, released when it leaves.
   val player = remember(uri) {
      ExoPlayer.Builder(context).build().apply {
         setMediaItem(MediaItem.fromUri(uri))
         prepare()
         playWhenReady = true
      }
   }
   DisposableEffect(player) {
      onDispose { player.release() }
   }

   // Pause when the app goes to the background.
   LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
      player.pause()
   }

   val presentationState = rememberPresentationState(player)
   val playPauseState = rememberPlayPauseButtonState(player)

   Box(modifier
      .fillMaxSize()
      .background(Color.Black)
   ) {
      PlayerSurface(
         player = player,
         modifier = Modifier.resizeWithContentScale(
            ContentScale.Fit, presentationState.videoSizeDp),
      )
      // Hide the first frames until the video is ready to avoid a flicker.
      if (presentationState.coverSurface) {
         Box(Modifier
            .matchParentSize()
            .background(Color.Black))
      }
      Row(
         modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
         horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
         // Pause
         IconButton(
            onClick = playPauseState::onClick,
            enabled = playPauseState.isEnabled,
         ) {
            Icon(
               imageVector = if (playPauseState.showPlay) Icons.Filled.PlayArrow
               else Icons.Filled.Pause,
               contentDescription = stringResource(
                  if (playPauseState.showPlay) R.string.action_play else R.string.action_pause),
               tint = Color.White,        // the player background is black
            )
         }

         // Select other video
         IconButton(onClick = onSelectOther) {
            Icon(
               imageVector = Icons.Filled.VideoLibrary,
               contentDescription = stringResource(R.string.action_select_other_video),
               tint = Color.White,
            )
         }

         // close
         IconButton(onClick = onClose) {
            Icon(
               imageVector = Icons.Filled.Close,
               contentDescription = stringResource(R.string.action_close),
               tint = Color.White,
            )
         }
      }
   }
}