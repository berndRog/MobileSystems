package de.rogallab.mobile.ui.camerax

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.StringRes
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileDescriptorOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.IImageMediaStore
import de.rogallab.mobile.shared.domain.images.IVideoMediaStore
import de.rogallab.mobile.shared.domain.images.PendingVideo
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.usecases.PhotoUcSave
import de.rogallab.mobile.domain.usecases.VideoUcFinish
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(
   application: Application,
   private val _imageFileStorage: IImageFileStorage,
   private val _videoMediaStore: IVideoMediaStore,
   private val _photoUcSave: PhotoUcSave,
   private val _videoUcFinish: VideoUcFinish,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<CameraEffect>,
) : AndroidViewModel(application),
   IEffectSource<CameraEffect> by _effectDelegate {

   // State: the only persistent output of this ViewModel
   private val _stateFlow = MutableStateFlow(CameraUiState())
   val stateFlow: StateFlow<CameraUiState> = _stateFlow.asStateFlow()

   // Scope that outlives the ViewModel: a recording stopped in onCleared() is finalized
   // asynchronously, and the photo or video must still be published or discarded.
   private val finishScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

   // CameraX use cases
   private val preview = Preview.Builder().build().apply {
      setSurfaceProvider { request ->
         _stateFlow.update { it.copy(surfaceRequest = request) }
      }
   }
   private val imageCapture = ImageCapture.Builder().build()
   private val recorder = Recorder.Builder()
      .setQualitySelector(
         QualitySelector.from(
            Quality.HD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)))
      .build()
   private val videoCapture = VideoCapture.withOutput(recorder)

   private var activeRecording: Recording? = null
   private var pendingVideo: PendingVideo? = null
   private var isPreparingVideo = false            // guards against double clicks

   // Dispatcher: single public entry point for all events coming from the UI layer.
   fun onIntent(intent: CameraIntent) {
      when (intent) {
         CameraIntent.TakePhoto -> takePhoto()
         is CameraIntent.ToggleRecording -> toggleRecording(intent.audioGranted)
         CameraIntent.PlayVideo -> _stateFlow.update { it.copy(isPlayerVisible = it.lastVideoUri != null) }
         CameraIntent.ClosePlayer -> _stateFlow.update { it.copy(isPlayerVisible = false) }
      }
   }

   // Exception to the intent rule: this is a suspend function bound to the lifecycle
   // of the composition, not a user action. It suspends until the caller is cancelled.
   suspend fun bindToCamera(lifecycleOwner: LifecycleOwner) {
      val provider = ProcessCameraProvider.awaitInstance(getApplication())
      provider.unbindAll()
      provider.bindToLifecycle(
         lifecycleOwner,
         CameraSelector.DEFAULT_BACK_CAMERA,
         preview, imageCapture, videoCapture
      )
      try {
         awaitCancellation()
      } finally {
         provider.unbindAll()
      }
   }

   // ---------------------------------------------------------------- photo

   private fun takePhoto() {
      val app = getApplication<Application>()

      viewModelScope.launch {
         // 1. Create an empty private JPEG file as the target of the capture.
         val imagePath = _imageFileStorage.createCameraImageFile()
            .getOrElse { throwable ->
               Alog.e(TAG, "createCameraImageFile failed: ${throwable.message}")
               emitError(R.string.camera_photo_failed)
               return@launch
            }
            .imagePath

         // 2. CameraX writes the JPEG directly into the private file.
         val output = ImageCapture.OutputFileOptions.Builder(File(imagePath)).build()

         try {
            imageCapture.takePicture(
               output,
               ContextCompat.getMainExecutor(app),
               object : ImageCapture.OnImageSavedCallback {
                  override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                     finishScope.launch { finishPhoto(imagePath) }
                  }
                  override fun onError(e: ImageCaptureException) {
                     Alog.e(TAG, "takePhoto failed: ${e.message}")
                     finishScope.launch {
                        _imageFileStorage.deleteImageFromAppStorage(imagePath)
                        emitError(R.string.camera_photo_failed)
                     }
                  }
               }
            )
         } catch (e: Exception) {
            Alog.e(TAG, "takePicture failed to start: ${e.message}")
            _imageFileStorage.deleteImageFromAppStorage(imagePath)
            emitError(R.string.camera_photo_failed)
         }
      }
   }

   // 3. Verify the private file, export it to the gallery, remove the private copy.
   private suspend fun finishPhoto(imagePath: String) {
      _photoUcSave(imagePath, ALBUM, "IMG_${timestamp()}")
         .onSuccess { emitMessage(R.string.camera_photo_saved) }
         .onFailure {
            Alog.e(TAG, "save photo failed: ${it.message}")
            emitError(R.string.camera_photo_failed)
         }
   }

   // ---------------------------------------------------------------- video

   // The caller checks RECORD_AUDIO, without it the video is recorded without sound.
   @SuppressLint("MissingPermission")
   private fun toggleRecording(audioGranted: Boolean) {
      val running = activeRecording
      if (running != null) {
         running.stop()                  // the Finalize event ends the recording
         return
      }
      if (isPreparingVideo) return
      isPreparingVideo = true

      viewModelScope.launch {
         val app = getApplication<Application>()

         // 1. hidden MediaStore entry plus writable descriptor
         val pending = _videoMediaStore
            .createPendingVideo(groupName = ALBUM, fileName = "VID_${timestamp()}")
            .getOrElse {
               Alog.e(TAG, "createPendingVideo failed: ${it.message}")
               emitError(R.string.camera_video_failed)
               isPreparingVideo = false
               return@launch
            }
         pendingVideo = pending

         // 2. the recorder writes directly into the descriptor
         try {
            val output = FileDescriptorOutputOptions.Builder(pending.fileDescriptor).build()
            var prepared = recorder.prepareRecording(app, output)
            if (audioGranted) prepared = prepared.withAudioEnabled()

            activeRecording = prepared.start(ContextCompat.getMainExecutor(app)) { event ->
               when (event) {
                  is VideoRecordEvent.Start ->
                     _stateFlow.update { it.copy(isRecording = true) }

                  is VideoRecordEvent.Finalize -> {
                     activeRecording = null
                     _stateFlow.update { it.copy(isRecording = false) }
                     val finished = pendingVideo
                     pendingVideo = null
                     finishScope.launch { finishVideo(finished, event) }
                  }
                  else -> Unit
               }
            }
         } catch (e: Exception) {
            Alog.e(TAG, "start recording failed: ${e.message}")
            pendingVideo = null
            pending.close()
            _videoMediaStore.deleteVideoFromMediaStore(pending.uri)
            emitError(R.string.camera_video_failed)
         } finally {
            isPreparingVideo = false
         }
      }
   }

   // 3. publish a finished video or discard a failed one
   private suspend fun finishVideo(
      pending: PendingVideo?,
      event: VideoRecordEvent.Finalize
   ) {
      if (pending == null) return
      if (event.hasError()) Alog.e(TAG, "recording failed, error code ${event.error}")

      _videoUcFinish(pending, recordedSuccessfully = !event.hasError())
         .onSuccess { emitMessage(R.string.camera_video_saved) }
         .onFailure { emitError(R.string.camera_video_failed) }
   }

   // ---------------------------------------------------------------- effects

   // Builds the text with IStringProvider and emits it as a one-time effect.
   private suspend fun emitMessage(@StringRes resId: Int) = _effectDelegate.emit(
      CameraEffect.ShowMessage(_stringProvider.getString(resId))
   )

   private suspend fun emitError(@StringRes resId: Int) = _effectDelegate.emit(
      CameraEffect.ShowError(_stringProvider.getString(resId))
   )

   override fun onCleared() {
      activeRecording?.stop()           // Finalize still runs and publishes the video
      super.onCleared()
   }

   private fun timestamp(): String =
      SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
         .format(System.currentTimeMillis())

   companion object {
      private const val TAG = "<-CameraViewModel"
      private const val ALBUM = "A4_02_CameraX"
   }
}