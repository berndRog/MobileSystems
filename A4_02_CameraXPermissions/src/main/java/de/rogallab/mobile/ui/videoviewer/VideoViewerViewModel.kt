package de.rogallab.mobile.ui.videoviewer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoViewerViewModel : ViewModel() {

   private val _stateFlow = MutableStateFlow(VideoViewerUiState())
   val stateFlow: StateFlow<VideoViewerUiState> = _stateFlow.asStateFlow()

   // Dispatcher: single public entry point for all events coming from the UI layer.
   fun onIntent(intent: VideoViewerIntent) {
      when (intent) {
         is VideoViewerIntent.VideoSelected ->
            _stateFlow.update { videoViewerState ->  videoViewerState.copy(videoUri = intent.uri) }
         VideoViewerIntent.ClearVideo ->
            _stateFlow.update { videoViewerState -> videoViewerState.copy(videoUri = null) }
      }
   }
}