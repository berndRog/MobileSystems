package de.rogallab.mobile.shared.ui.images

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
import androidx.compose.ui.unit.dp

/**
 * Displays the current image and delegates all image actions to callbacks.
 */
@Composable
fun ImageSelection(
   fullName: String,
   imagePath: String?,
   imageActionsEnabled: Boolean = true,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit,
) {
   Row(
      modifier = Modifier
         .padding(top = 16.dp)
         .height(220.dp)
         .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      // Renders the current image or a placeholder icon if no image is available.
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
         // Renders the buttons for selecting a photo from galleyr or
         // taking a photo with camera or removing a photo.
         ImageSelectionButtons(
            imagePath = imagePath,
            enabled = imageActionsEnabled,
            onSelectPhoto = onSelectPhoto,
            onTakePhoto = onTakePhoto,
            onRemovePhoto = onRemovePhoto,
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ImageSelection ist jetzt eine rein darstellende Compose-Komponente.
 * - Sie kennt weder ActivityResultLauncher noch IImageFileStorage oder Koin.
 * - GalleryPickerHandler und CameraPickerHandler werden im Adapter verbunden.
 */
