package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.shared.domain.images.CameraImageFile
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.ImageFileFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerResultsInstrumentedTest {
   @get:Rule val compose = createComposeRule()
   private val results = ControlledResults()

   @Test fun galleryResultForwardsUriExactlyOnce() {
      val selected = mutableListOf<Uri>()
      gallery { selected += it }
      compose.onNodeWithText("gallery").performClick()
      val uri = Uri.parse("content://test/gallery/image.jpg")
      compose.runOnIdle { results.complete(uri) }
      compose.runOnIdle { assertEquals(listOf(uri), selected) }
      assertEquals(1, results.launches.size)
   }

   @Test fun cancelledGalleryResultKeepsSelection() {
      val existing = Uri.parse("content://test/existing.jpg")
      val selected = mutableListOf(existing)
      gallery { selected += it }
      compose.onNodeWithText("gallery").performClick()
      compose.runOnIdle { results.complete<Uri?>(null) }
      compose.runOnIdle { assertEquals(listOf(existing), selected) }
   }

   @Test fun successfulCameraResultConfirmsBeforeForwardingInternalPath() {
      val storage = RecordingStorage()
      val selected = mutableListOf<String>()
      camera(storage, selected)
      launchCamera()
      compose.runOnIdle { results.complete(true) }
      compose.waitUntil(2_000) { selected.isNotEmpty() }
      compose.runOnIdle {
         assertEquals(listOf(storage.path), storage.confirmed)
         assertEquals(listOf(storage.path), selected)
         assertTrue(storage.deleted.isEmpty())
      }
   }

   @Test fun cancelledCameraResultDeletesProvisionalFileWithoutSelection() {
      val storage = RecordingStorage()
      val selected = mutableListOf<String>()
      camera(storage, selected)
      launchCamera()
      compose.runOnIdle { results.complete(false) }
      compose.waitUntil(2_000) { storage.deleted.isNotEmpty() }
      compose.runOnIdle {
         assertEquals(listOf(storage.path), storage.deleted)
         assertTrue(storage.confirmed.isEmpty())
         assertTrue(selected.isEmpty())
      }
   }

   @Test fun failedCameraConfirmationDeletesProvisionalFileWithoutSelection() {
      val storage = RecordingStorage().apply { confirmationFails = true }
      val selected = mutableListOf<String>()
      val errors = mutableListOf<Throwable>()
      camera(storage, selected, errors)
      launchCamera()
      compose.runOnIdle { results.complete(true) }
      compose.waitUntil(2_000) { storage.deleted.isNotEmpty() }
      compose.runOnIdle {
         assertEquals(listOf(storage.path), storage.confirmed)
         assertEquals(listOf(storage.path), storage.deleted)
         assertTrue(selected.isEmpty())
         assertEquals(1, errors.size)
      }
   }

   private fun gallery(onSelected: (Uri) -> Unit) {
      compose.setContent {
         CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
            GalleryPickerHandlerSingle(onImageSelected = onSelected) { actions ->
               Button(onClick = actions.selectFromGallery) { Text("gallery") }
            }
         }
      }
   }

   private fun camera(
      storage: RecordingStorage,
      selected: MutableList<String>,
      errors: MutableList<Throwable> = mutableListOf(),
   ) {
      compose.setContent {
         CompositionLocalProvider(LocalActivityResultRegistryOwner provides results) {
            CameraPickerHandler(storage, selected::add, errors::add) { actions ->
               Button(onClick = actions.takePhoto, enabled = !actions.isBusy) { Text("camera") }
            }
         }
      }
   }

   private fun launchCamera() {
      compose.onNodeWithText("camera").performClick()
      compose.waitUntil(2_000) { results.launches.isNotEmpty() }
      assertEquals(1, results.launches.size)
      assertEquals(Uri.parse("content://test/camera.jpg"), results.launches.single())
   }

   private class ControlledResults : ActivityResultRegistryOwner {
      val launches = mutableListOf<Any?>()
      private var requestCode: Int? = null
      override val activityResultRegistry = object : ActivityResultRegistry() {
         override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
         ) {
            this@ControlledResults.requestCode = requestCode
            launches += input
         }
      }

      fun <T> complete(value: T) {
         activityResultRegistry.dispatchResult(requireNotNull(requestCode), value)
         requestCode = null
      }
   }

   private class RecordingStorage : IImageFileStorage {
      val path = "/private/test/camera.jpg"
      private val uri = Uri.parse("content://test/camera.jpg")
      var confirmationFails = false
      val confirmed = mutableListOf<String>()
      val deleted = mutableListOf<String?>()

      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
         Result.failure(UnsupportedOperationException())
      override suspend fun createCameraImageFile(): Result<CameraImageFile> =
         Result.success(CameraImageFile(path, uri))
      override suspend fun confirmCameraImageFile(imagePath: String): Result<String> {
         confirmed += imagePath
         return if (confirmationFails) Result.failure(IllegalStateException("empty image"))
         else Result.success(imagePath)
      }
      override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> {
         deleted += imagePath
         return Result.success(Unit)
      }
      override suspend fun saveDrawableToAppStorage(
         drawableResId: Int, fileName: String, format: ImageFileFormat, quality: Int,
      ): Result<String> = Result.failure(UnsupportedOperationException())
   }
}
