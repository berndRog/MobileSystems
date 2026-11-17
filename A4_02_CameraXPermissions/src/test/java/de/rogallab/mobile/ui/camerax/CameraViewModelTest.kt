package de.rogallab.mobile.ui.camerax

import android.app.Application
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.usecases.PhotoUcSave
import de.rogallab.mobile.domain.usecases.VideoUcFinish
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.*
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
   shadows = [CameraViewModelTest.CaptureShadow::class],
   instrumentedPackages = ["androidx.camera.core"],
)
class CameraViewModelTest {
   private val dispatcher = StandardTestDispatcher()
   private val storage = RecordingStorage()
   private val mediaStore = RecordingMediaStore()
   private val store = ViewModelStore()
   private lateinit var viewModel: CameraViewModel

   @Before
   fun setUp() {
      Dispatchers.setMain(dispatcher)
      CaptureShadow.request = null
      CaptureShadow.startFailure = null
      val videoStore = UnusedVideoStore()
      viewModel = CameraViewModel(
         ApplicationProvider.getApplicationContext(),
         storage,
         videoStore,
         PhotoUcSave(storage, mediaStore),
         VideoUcFinish(videoStore),
         object : IStringProvider {
            override fun getString(resId: Int, vararg args: Any): String = resId.toString()
         },
         EffectDelegate(),
      )
      store.put("camera", viewModel)
   }

   @After
   fun tearDown() {
      store.clear()
      Dispatchers.resetMain()
      CaptureShadow.request = null
      CaptureShadow.startFailure = null
   }

   @Test
   fun initialState_hasNoPreviewRecordingOrPublishedVideo() {
      assertNull(viewModel.stateFlow.value.surfaceRequest)
      assertFalse(viewModel.stateFlow.value.isRecording)
      assertNull(viewModel.stateFlow.value.lastVideoUri)
      assertEquals(0, storage.privateCalls)
   }

   @Test
   fun takePhoto_exportFailure_deletesPrivatePathAndEmitsError() = runTest(dispatcher) {
      mediaStore.exportFails = true
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()
         requireNotNull(CaptureShadow.request).callback.onImageSaved(
            ImageCapture.OutputFileResults(null)
         )
         runCurrent()

         assertEquals(listOf(storage.path), storage.confirmed)
         assertEquals(listOf(storage.path), mediaStore.exported)
         assertEquals(listOf(storage.path), storage.deleted)
         assertEquals(CameraEffect.ShowError(R.string.camera_photo_failed.toString()), awaitItem())
         assertFalse(viewModel.stateFlow.value.isRecording)
         assertNull(viewModel.stateFlow.value.lastVideoUri)
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_privateFileFailure_emitsErrorAndStopsBeforeCapture() = runTest(dispatcher) {
      storage.creation = Result.failure(java.io.IOException("disk full"))
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()

         assertEquals(CameraEffect.ShowError(R.string.camera_photo_failed.toString()), awaitItem())
         assertEquals(1, storage.privateCalls)
         assertNull(CaptureShadow.request)
         assertTrue(storage.confirmed.isEmpty())
         assertTrue(storage.deleted.isEmpty())
         assertTrue(mediaStore.exported.isEmpty())
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_passesPrivatePathToCameraXAndWaitsForCallback() = runTest(dispatcher) {
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()

         assertEquals(1, storage.privateCalls)
         assertEquals(File(storage.path), requireNotNull(CaptureShadow.request).output.file)
         assertTrue(storage.confirmed.isEmpty())
         assertTrue(storage.deleted.isEmpty())
         assertTrue(mediaStore.exported.isEmpty())
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_savedCallback_confirmsExportsAndDeletesReturnedPath() = runTest(dispatcher) {
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()
         requireNotNull(CaptureShadow.request).callback.onImageSaved(
            ImageCapture.OutputFileResults(null)
         )
         runCurrent()

         assertEquals(listOf(storage.path), storage.confirmed)
         assertEquals(listOf(storage.path), mediaStore.exported)
         assertEquals(listOf(storage.path), storage.deleted)
         assertEquals(CameraEffect.ShowMessage(R.string.camera_photo_saved.toString()), awaitItem())
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_captureError_deletesPrivatePathWithoutPublishing() = runTest(dispatcher) {
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()
         requireNotNull(CaptureShadow.request).callback.onError(
            ImageCaptureException(ImageCapture.ERROR_CAPTURE_FAILED, "capture failed", null)
         )
         runCurrent()

         assertEquals(listOf(storage.path), storage.deleted)
         assertTrue(storage.confirmed.isEmpty())
         assertTrue(mediaStore.exported.isEmpty())
         assertEquals(CameraEffect.ShowError(R.string.camera_photo_failed.toString()), awaitItem())
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_startThrows_deletesPrivatePathAndEmitsError() = runTest(dispatcher) {
      CaptureShadow.startFailure = IllegalStateException("camera unavailable")
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()

         assertEquals(listOf(storage.path), storage.deleted)
         assertTrue(storage.confirmed.isEmpty())
         assertTrue(mediaStore.exported.isEmpty())
         assertEquals(CameraEffect.ShowError(R.string.camera_photo_failed.toString()), awaitItem())
         expectNoEvents()
      }
   }

   @Test
   fun takePhoto_confirmationFailure_deletesPrivatePathAndDoesNotExport() = runTest(dispatcher) {
      storage.confirmationFails = true
      viewModel.effects.test {
         viewModel.onIntent(CameraIntent.TakePhoto)
         runCurrent()
         requireNotNull(CaptureShadow.request).callback.onImageSaved(
            ImageCapture.OutputFileResults(null)
         )
         runCurrent()

         assertEquals(listOf(storage.path), storage.confirmed)
         assertEquals(listOf(storage.path), storage.deleted)
         assertTrue(mediaStore.exported.isEmpty())
         assertEquals(CameraEffect.ShowError(R.string.camera_photo_failed.toString()), awaitItem())
         expectNoEvents()
      }
   }

   // Intercept only the hardware boundary; builders and the ViewModel's callback
   // and use-case orchestration remain real. No camera device is needed.
   @Implements(ImageCapture::class)
   class CaptureShadow {
      data class Request(
         val output: ImageCapture.OutputFileOptions,
         val callback: ImageCapture.OnImageSavedCallback,
      )

      @Implementation
      fun takePicture(
         output: ImageCapture.OutputFileOptions,
         executor: Executor,
         callback: ImageCapture.OnImageSavedCallback,
      ) {
         startFailure?.let { throw it }
         request = Request(output, callback)
      }

      companion object {
         var request: Request? = null
         var startFailure: RuntimeException? = null
      }
   }

   private class RecordingStorage : IImageFileStorage {
      val path = "/private/camerax/returned-image.jpg"
      var creation: Result<String> = Result.success(path)
      var privateCalls = 0
      var confirmationFails = false
      val confirmed = mutableListOf<String>()
      val deleted = mutableListOf<String?>()

      override suspend fun createPrivateImageFile(): Result<String> {
         privateCalls++
         return creation
      }
      override suspend fun createCameraImageFile(): Result<CameraImageFile> =
         error("CameraX must not request a FileProvider Uri")
      override suspend fun confirmCameraImageFile(imagePath: String): Result<String> {
         confirmed += imagePath
         return if (confirmationFails) Result.failure(IllegalStateException("empty image"))
         else Result.success(imagePath)
      }
      override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
         deleted += imagePath
         return Result.success(Unit)
      }
      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> = error("unused")
      override suspend fun saveDrawableToAppStorage(
         drawableResId: Int, fileName: String, format: ImageFileFormat, quality: Int,
      ): Result<String> = error("unused")
   }

   private class RecordingMediaStore : IImageMediaStore {
      val exported = mutableListOf<String>()
      var exportFails = false
      override suspend fun exportImageToMediaStore(
         imagePath: String, groupName: String, fileName: String?,
      ): Result<Uri> {
         exported += imagePath
         assertEquals("A4_02_CameraX", groupName)
         assertTrue(requireNotNull(fileName).startsWith("IMG_"))
         return if (exportFails) Result.failure(java.io.IOException("gallery unavailable"))
         else Result.success(Uri.parse("content://test/saved"))
      }
      override suspend fun deleteImageFromMediaStore(imageUri: Uri): Result<Unit> = error("unused")
   }

   private class UnusedVideoStore : IVideoMediaStore {
      override suspend fun createPendingVideo(
         groupName: String, fileName: String?,
      ): Result<PendingVideo> = error("unused")
      override suspend fun publishVideo(videoUri: Uri): Result<Uri> = error("unused")
      override suspend fun deleteVideoFromMediaStore(videoUri: Uri): Result<Unit> = error("unused")
   }
}
