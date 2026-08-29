package de.rogallab.mobile.ui.people.input_detail.composables

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import java.io.File

@Composable
fun PersonImageSelection(
   person: Person,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PersonImageSel"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

   val contentDescription = person.displayName
      .takeUnless(String::isBlank)
      ?.let { stringResource(R.string.person_image_named, it) }
      ?: stringResource(R.string.person_image)

   Row(
      modifier = modifier
         .padding(top = 16.dp)
         .height(220.dp)
         .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      ImageRenderer(
         modifier = modifier
            .weight(0.5f)
            .fillMaxWidth()
            .height(220.dp),
         imageVector = Icons.Default.AccountCircle,
         imagePath = person.imagePath,
         contentDescription = contentDescription,
      )

      ImageSelectionButtons(
         modifier = modifier
            .weight(0.5f)
            .fillMaxWidth(),
         imagePath = person.imagePath,
         onSelectPhoto = onSelectPhoto,
         onTakePhoto = onTakePhoto,
         onRemovePhoto = onRemovePhoto
      )

   }
}