package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Selects one image and forwards its content URI. */
@Composable
fun GalleryPickerHandlerSingle(
   onImageSelected: (Uri) -> Unit,
   content: @Composable (GalleryPickerActions) -> Unit,
) {
   // Keep the latest callback available after recomposition.
   val currentOnImageSelected = rememberUpdatedState(onImageSelected)
   val imageRequest = remember {
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
   }
   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
   ) { uri ->
      // Closing the picker does not emit an event.
      uri?.let { currentOnImageSelected.value(it) }
   }
   content(GalleryPickerActions(
      selectFromGallery = { launcher.launch(imageRequest) },
   ))
}

/*
 * Didaktik und Lernziele
 *
 * - PickVisualMedia liefert höchstens eine Content-Uri.
 * - Erst selectFromGallery öffnet den Photo Picker.
 * - Das ViewModel kopiert das gewählte Bild über IImageFileStorage.
 */
