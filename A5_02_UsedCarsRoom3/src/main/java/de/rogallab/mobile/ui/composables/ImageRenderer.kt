package de.rogallab.mobile.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.toImageModel

@Composable
fun ImageRenderer(
   modifier: Modifier = Modifier,
   imageVector: ImageVector,
   imagePath: String?,
   contentDescription: String? = null,
) {
   val tag = "<-ImageRenderer"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

   Surface(
      modifier = modifier,
      shape = RoundedCornerShape(10.dp)
   ) {
      if (imagePath.isNullOrBlank()) {
         Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Icon(
               imageVector = imageVector,
               contentDescription = contentDescription,
               modifier = Modifier.fillMaxHeight(),
            )
         }
      } else {
         AsyncImage(
            model = imagePath.toImageModel(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}