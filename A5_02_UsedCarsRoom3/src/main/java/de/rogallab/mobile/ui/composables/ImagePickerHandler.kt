package de.rogallab.mobile.ui.composables

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.copyImageToAppStorage
import de.rogallab.mobile.data.local.io.createCameraImageFile
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import kotlinx.coroutines.launch

class ImagePickerHandler(val openGalleryPicker: () -> Unit, val openCamera: () -> Unit)

@Composable
fun rememberImagePickerHandler(
   maxSelection: Int,
   onImagesSelected: (List<String>) -> Unit,
   onStorageFailed: (String) -> Unit,
): ImagePickerHandler {
   require(maxSelection > 0)
   val context = LocalContext.current
   val scope = rememberCoroutineScope()
   val imageSaveError = context.getString(R.string.error_image_save)
   var pendingCameraImagePath by rememberSaveable { mutableStateOf<String?>(null) }

   fun copySelectedUris(uris: List<Uri>) {
      if (uris.isEmpty()) return
      scope.launch {
         val paths = mutableListOf<String>()
         var failed = false
         uris.forEach { uri ->
            copyImageToAppStorage(context, uri)
               .onSuccess(paths::add)
               .onFailure { failed = true }
         }
         if (paths.isNotEmpty()) onImagesSelected(paths)
         if (failed) onStorageFailed(imageSaveError)
      }
   }

   val singlePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
      uri?.let { copySelectedUris(listOf(it)) }
   }
   val multiPicker = if (maxSelection > 1) {
      rememberLauncherForActivityResult(PickMultipleVisualMedia(maxItems = maxSelection)) { copySelectedUris(it) }
   } else null
   val camera = rememberLauncherForActivityResult(TakePicture()) { success ->
      val path = pendingCameraImagePath
      pendingCameraImagePath = null
      when {
         success && path != null -> onImagesSelected(listOf(path))
         path != null -> deleteImageFromAppStorage(path)
      }
   }

   return ImagePickerHandler(
      openGalleryPicker = {
         val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
         if (maxSelection > 1) multiPicker?.launch(request) else singlePicker.launch(request)
      },
      openCamera = {
         createCameraImageFile(context)
            .onSuccess { file ->
               pendingCameraImagePath = file.imagePath
               camera.launch(file.contentUri)
            }
            .onFailure { onStorageFailed(imageSaveError) }
      },
   )
}
