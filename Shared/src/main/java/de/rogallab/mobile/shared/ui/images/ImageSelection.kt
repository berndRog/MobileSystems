package de.rogallab.mobile.shared.ui.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Displays the current image and delegates all image actions to callbacks.
 */
@Composable
fun ImageSelection(
   fullName: String,
   imagePath: String?,
   imageActionsEnabled: Boolean = true,
   height: Dp = 250.dp,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit
) {
   Row(
      modifier = Modifier
         .padding(vertical = 16.dp)
         .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      // Renders the current image or a placeholder icon if no image is available.
      ImageRenderer(
         modifier = Modifier
            .weight(1f)
            .heightIn(min = 150.dp)
            .height(height),
         imageVector = Icons.Default.AccountCircle,
         imagePath = imagePath,
         contentDescription = fullName,
      )

      // Renders the buttons for selecting a photo from galleyr or
      // taking a photo with camera or removing a photo.
      ImageSelectionButtons(
         modifier = Modifier
            .weight(1f)
            .heightIn(min = 150.dp)
            .height(height),
         imagePath = imagePath,
         enabled = imageActionsEnabled,
         onSelectPhoto = onSelectPhoto,
         onTakePhoto = onTakePhoto,
         onRemovePhoto = onRemovePhoto,
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ImageSelection ist jetzt eine rein darstellende Compose-Komponente.
 * - Sie kennt weder ActivityResultLauncher noch IImageFileStorage oder Koin.
 * - SingleGalleryPickerHandler und MultipleGalleryPickerHandler und CameraPickerHandler werden im Adapter verbunden.
 */
