package de.rogallab.mobile.shared.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.R
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import org.koin.compose.koinInject

/**
 * Combines image rendering with gallery, camera and remove actions.
 */
@Composable
fun ImageSelection(
   fullName: String,
   imagePath: String?,
   onImageChange: (String?) -> Unit,
   onFailure: (String) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val imageSaveError = stringResource(R.string.error_image_save)

   // ImagePickerHandler stores selected images before forwarding their paths.
   ImagePickerHandler(
      imageFileStorage = imageFileStorage,
      selectionMode = ImageSelectionMode.Single,
      onImagesStored = { imagePaths -> onImageChange(imagePaths.firstOrNull()) },
      onError = { onFailure(imageSaveError) },
   ) { imagePickerActions ->

      // Show the current image next to the available image actions.
      Row(
         modifier = Modifier
            .padding(top = 16.dp)
            .height(220.dp)
            .fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
         ImageRenderer(
            modifier = Modifier.weight(1f),
            imageVector = Icons.Default.AccountCircle,
            imagePath = imagePath,
            contentDescription = fullName,
         )

         Column(
            modifier = Modifier
               .weight(1f)
               .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
         ) {
            ImageSelectionButtons(
               imagePath     = imagePath,
               onSelectPhoto = imagePickerActions.selectFromGallery,
               onTakePhoto   = imagePickerActions.takePhoto,
               onRemovePhoto = { onImageChange(null) },
            )
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ImageSelection fasst Bildanzeige und Bildauswahl zu einer wiederverwendbaren
 *   UI-Komponente zusammen.
 *
 * - Der Screen erhält nur einen gespeicherten Dateipfad. Die technische Arbeit
 *   mit Activity-Result-Launchern, Content-URIs und Kamera-Dateien bleibt im
 *   ImagePickerHandler verborgen.
 *
 * - Galerie und Kamera verwenden dieselbe Callback-Schnittstelle:
 *
 *      onImageChange(String?)
 *
 * - Fehler werden als String zurückgegeben und können dadurch direkt über die
 *   vorhandene Effect-/SnackbarController-Struktur angezeigt werden.
 *
 * Lernziele:
 *
 * - Technische Activity-Result-Logik aus fachlichen Screens auslagern.
 * - Galerie und Kamera über eine gemeinsame UI-Schnittstelle verwenden.
 * - Gespeicherte Dateipfade statt kurzlebiger Content-URIs weiterreichen.
 */
