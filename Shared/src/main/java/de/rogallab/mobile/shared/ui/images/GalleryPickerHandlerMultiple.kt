package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** Selects up to maxSelectionCount images and forwards their content URIs. */
@Composable
fun GalleryPickerHandlerMultiple(
   maxSelectionCount: Int,
   onImagesSelected: (List<Uri>) -> Unit,
   content: @Composable (GalleryPickerActions) -> Unit,
) {
   // The multi-select contract requires room for at least two images.
   require(maxSelectionCount >= 2)
   val currentOnImagesSelected = rememberUpdatedState(onImagesSelected)
   val imageRequest = remember {
      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
   }
   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(
         maxItems = maxSelectionCount,
      ),
   ) { uris ->
      // Closing the picker does not emit an event.
      if (uris.isNotEmpty()) currentOnImagesSelected.value(uris)
   }
   content(GalleryPickerActions(
      selectFromGallery = { launcher.launch(imageRequest) },
   ))
}

/*
 * Didaktik und Lernziele
 *
 * - PickMultipleVisualMedia liefert eine Liste von Content-URIs.
 * - Der Aufrufer begrenzt die Auswahl auf die noch freien Bildplätze.
 * - Bei nur einem freien Platz wird SingleGalleryPickerHandler verwendet.
 * - Das ViewModel kopiert die Bilder über IImageFileStorage.
 */
