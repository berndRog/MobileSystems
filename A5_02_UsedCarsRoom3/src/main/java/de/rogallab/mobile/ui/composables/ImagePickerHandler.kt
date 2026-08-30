// ui/composables/ImagePickerHandler.kt
package de.rogallab.mobile.ui.composables

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.copyImageToAppStorage
import de.rogallab.mobile.data.local.io.createCameraImageFile
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import kotlinx.coroutines.launch

// Return Type for the rememberImagePickerHandler() composable.
class ImagePickerHandler(
   val openGalleryPicker: () -> Unit,
   val openCamera: () -> Unit,
)

@Composable
fun rememberImagePickerHandler(
   maxSelection: Int,
   onImagesSelected: (List<String>) -> Unit,
   onStorageFailed: (UiText) -> Unit,
): ImagePickerHandler {

   val tag = "<-ImagePickerHandler"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

   require(maxSelection > 0) { "maxSelection must be > 0" }

   val context = LocalContext.current
   val coroutineScope = rememberCoroutineScope()
   val imageSaveError = uiText(R.string.error_image_save)

   var pendingCameraImagePath by rememberSaveable { mutableStateOf<String?>(null) }

   // Copy the selected images to the app's storage and call the callback with the new paths.
   fun copySelectedUris(uris: List<Uri>) {
      if (uris.isEmpty()) return
      coroutineScope.launch {
         val newImagePaths = mutableListOf<String>()
         var copyFailed = false

         uris.forEach { uri ->
            copyImageToAppStorage(context, uri)
               .onSuccess { newImagePaths.add(it) }
               .onFailure { copyFailed = true }
         }

         if (newImagePaths.isNotEmpty()) onImagesSelected(newImagePaths)
         if (copyFailed) onStorageFailed(imageSaveError)
      }
   }

   // Launchers for the gallery pickers.
   val singlePicker = rememberLauncherForActivityResult(
      contract = PickVisualMedia(),
   ) { uri ->
      if (uri != null) copySelectedUris(listOf(uri))
   }

   val multiPicker = if (maxSelection > 1) {
      rememberLauncherForActivityResult(
         contract = PickMultipleVisualMedia(maxItems = maxSelection),
      ) { uris ->
         copySelectedUris(uris)
      }
   } else {
      null
   }

   // Launcher for the camera.
   val camera = rememberLauncherForActivityResult(TakePicture()) { photoWasTaken ->
      val imagePath = pendingCameraImagePath
      pendingCameraImagePath = null

      when {
         photoWasTaken && imagePath != null -> onImagesSelected(listOf(imagePath))
         imagePath != null -> deleteImageFromAppStorage(imagePath)
      }
   }

   return ImagePickerHandler(
      openGalleryPicker = {
         if (maxSelection > 1) {
            multiPicker?.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
         } else {
            singlePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
         }
      },
      openCamera = {
         createCameraImageFile(context)
            .onSuccess {
               pendingCameraImagePath = it.imagePath
               camera.launch(it.contentUri)
            }
      },
   )

}