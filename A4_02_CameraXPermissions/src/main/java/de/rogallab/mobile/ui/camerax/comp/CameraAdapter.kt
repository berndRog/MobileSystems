package de.rogallab.mobile.ui.camerax.comp

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.effects.rememberSnackbarController
import de.rogallab.mobile.ui.camerax.CameraAndAudioPermissionsHandler
import de.rogallab.mobile.ui.camerax.CameraEffect
import de.rogallab.mobile.ui.camerax.CameraIntent
import de.rogallab.mobile.ui.camerax.CameraViewModel
import org.koin.androidx.compose.koinViewModel

// Stateful: connects ViewModel, lifecycle and permissions with the stateless screen.
@Composable
fun CameraAdapter(
   viewModel: CameraViewModel = koinViewModel(),
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   modifier: Modifier = Modifier
) {
   val lifecycleOwner = LocalLifecycleOwner.current
   val cameraUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   val snackbarHostState = remember { SnackbarHostState() }
   val snackbarController = rememberSnackbarController(
      snackbarHostState = snackbarHostState,
   )
   val onMessage = snackbarController::showMessage
   val onError = snackbarController::showError

   // Handle one-time effects separately from the persistent UI state.
   EffectHandler(viewModel.effects) { cameraEffect ->
      when (cameraEffect) {
         is CameraEffect.ShowMessage -> onMessage(cameraEffect.message)
         is CameraEffect.ShowError -> onError(cameraEffect.message)
      }
   }


   CameraAndAudioPermissionsHandler(
      modifier = modifier
   ) { audioGranted ->

      // Only composed while the CAMERA permission is granted.
      LaunchedEffect(lifecycleOwner) { viewModel.bindToCamera(lifecycleOwner) }

      CameraScreen(
         surfaceRequest = cameraUiState.surfaceRequest,
         isRecording = cameraUiState.isRecording,
         audioGranted = audioGranted,
         // Events go to the ViewModel as intents.
         onTakePhoto = { viewModel.onIntent(CameraIntent.TakePhoto) },
         onToggleRecording = {
            viewModel.onIntent(CameraIntent.ToggleRecording(audioGranted))
         }
      )
   }
}