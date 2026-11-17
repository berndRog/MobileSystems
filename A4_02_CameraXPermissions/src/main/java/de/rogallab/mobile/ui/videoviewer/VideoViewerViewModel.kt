package de.rogallab.mobile.ui.videoviewer

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VideoViewerViewModel(
   initialVideoUri: String?,
) : ViewModel() {

   // The video passed by the route is the start value of the state.
   private val _stateFlow = MutableStateFlow(
      VideoViewerUiState(videoUri = initialVideoUri?.toUri())   // androidx.core.net.toUri
   )
   val stateFlow: StateFlow<VideoViewerUiState> = _stateFlow.asStateFlow()

   init { Alog.v(TAG, "VideoViewerViewModel created, initialVideoUri = $initialVideoUri") }

   // Dispatcher: single public entry point for all events coming from the UI layer.
   fun onIntent(intent: VideoViewerIntent) {
      when (intent) {
         is VideoViewerIntent.VideoSelected ->
            _stateFlow.update { it.copy(videoUri = intent.uri) }
         VideoViewerIntent.ClearVideo ->
            _stateFlow.update { it.copy(videoUri = null) }
      }
   }

   companion object {
      private const val TAG = "<-VideoViewerVm"
   }
}