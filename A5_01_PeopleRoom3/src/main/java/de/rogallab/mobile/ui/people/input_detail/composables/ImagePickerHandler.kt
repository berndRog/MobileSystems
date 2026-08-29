package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.copyImageToAppStorage
import de.rogallab.mobile.data.local.io.createCameraImageFile
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.ui.common.uiText
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import kotlinx.coroutines.launch

class ImagePickerHandler(
   val openPhotoPicker: () -> Unit,
   val openCamera: () -> Unit,
)

@Composable
fun rememberPersonImagePickerHandler(
   onIntent: (PersonIntent) -> Unit,
): ImagePickerHandler {
   val context = LocalContext.current
   val coroutineScope = rememberCoroutineScope()
   val imageSaveError = uiText(R.string.error_image_save)

   var pendingCameraImagePath by rememberSaveable {
      mutableStateOf<String?>(null)
   }

   val photoPicker = rememberLauncherForActivityResult(
      contract = PickVisualMedia(),
   ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      coroutineScope.launch {
         copyImageToAppStorage(context = context, sourceUri = uri)
            .onSuccess { imagePath -> onIntent(PersonIntent.ImageChanged(imagePath)) }
            .onFailure { onIntent(PersonIntent.ImageStorageFailed(imageSaveError)) }
      }
   }

   val camera = rememberLauncherForActivityResult(
      contract = TakePicture(),
   ) { photoWasTaken ->
      val imagePath = pendingCameraImagePath
      pendingCameraImagePath = null

      when {
         photoWasTaken && imagePath != null -> {
            onIntent(PersonIntent.ImageChanged(imagePath))
         }
         imagePath != null -> {
            deleteImageFromAppStorage(imagePath)
         }
      }
   }

   return ImagePickerHandler(
      openPhotoPicker = {
         photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
      },
      openCamera = {
         createCameraImageFile(context)
            .onSuccess { cameraImage ->
               pendingCameraImagePath = cameraImage.imagePath
               camera.launch(cameraImage.contentUri)
            }
            .onFailure {
               onIntent(PersonIntent.ImageStorageFailed(imageSaveError))
            }
      }
   )
}
