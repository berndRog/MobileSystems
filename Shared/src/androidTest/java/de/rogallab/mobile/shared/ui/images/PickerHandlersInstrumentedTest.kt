package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.shared.domain.io.CameraImageFile
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickerHandlersInstrumentedTest {

   @get:Rule
   val composeRule = createComposeRule()

   @Test
   fun galleryPickerHandler_singleMode_exposesContentAction() {
      composeRule.setContent {
         GalleryPickerHandler(
            selectionMode = GallerySelectionMode.Single,
            onImagesSelected = {},
         ) {
            Text("gallery-ready")
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNodeWithText("gallery-ready")
         .assertExists()
   }

   @Test
   fun galleryPickerHandler_multipleMode_acceptsSmallConfiguredMaximum() {
      composeRule.setContent {
         GalleryPickerHandler(
            selectionMode = GallerySelectionMode.Multiple,
            maxSelectionCount = 1,
            onImagesSelected = {},
         ) {
            Text("multiple-ready")
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNodeWithText("multiple-ready")
         .assertExists()
   }

   @Test
   fun cameraPickerHandler_fileCreationFailure_reportsErrorAndResetsBusyState() {
      val expected = IllegalStateException("camera file failed")
      val storage = FailingCameraStorage(expected)
      val errors = mutableListOf<Throwable>()

      composeRule.setContent {
         CameraPickerHandler(
            imageFileStorage = storage,
            onPhotoStored = {},
            onError = { throwable -> errors += throwable },
         ) { actions ->
            Button(
               onClick = actions.takePhoto,
               enabled = !actions.isBusy,
            ) {
               Text(
                  if (actions.isBusy) "camera-busy"
                  else "take-photo"
               )
            }
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNodeWithText("take-photo")
         .performClick()
      composeRule.waitForIdle()

      composeRule.waitUntil(timeoutMillis = 2_000) {
         errors.isNotEmpty()
      }

      composeRule.runOnIdle {
         assertEquals(listOf(expected), errors)
      }

      composeRule
         .onNodeWithText("take-photo")
         .assertExists()
   }

   private class FailingCameraStorage(
      private val failure: Throwable,
   ) : IImageFileStorage {

      override suspend fun copyImageToAppStorage(sourceUri: Uri): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun createCameraImageFile(): Result<CameraImageFile> =
         Result.failure(failure)

      override suspend fun confirmCameraImageFile(imagePath: String): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun saveDrawableToAppStorage(
         drawableResId: Int,
         fileName: String,
         format: ImageFileFormat,
         quality: Int,
      ): Result<String> =
         Result.failure(UnsupportedOperationException())

      override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
         Result.success(Unit)
   }
}
