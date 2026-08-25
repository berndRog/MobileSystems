package de.rogallab.mobile.ui.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun ExampleBox(
   modifier: Modifier = Modifier
) {
   Column(
      modifier = modifier
   ) {
      Text(
         modifier = Modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth(),
         text = "Box with Image and TextIcon on top of each other"
      )

      Box(
         modifier = Modifier.size(160.dp)
      ) {
         Image(
            painter = painterResource(R.drawable.person),
            contentDescription = "Bild einer Person",
            modifier = Modifier.fillMaxSize()
         )

         IconButton(
            onClick = {
               Alog.d("<-ExmapleBox","Bild entfernen")
            },
            modifier = Modifier.align(Alignment.BottomEnd)
         ) {
            Icon(
               modifier = Modifier
                  .size(32.dp)
                  .background(Color.White),
               imageVector = Icons.Filled.Close,
               contentDescription = "Bild entfernen"
            )
         }
      }
   }
}
