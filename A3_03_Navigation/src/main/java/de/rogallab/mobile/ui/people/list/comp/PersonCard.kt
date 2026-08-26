package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.images.ImageRenderer

@Composable
fun PersonCard(
   firstName: String,         // State ↓
   lastName: String,          // State ↓
   email: String?,            // State ↓
   phone: String?,            // State ↓
   imagePath: String?,        // State ↓
   onDetail: () -> Unit,      // Event ↑
   onDelete: () -> Unit,      // Event ↑
   modifier: Modifier = Modifier
) {
   val tag = "<-PersonCard"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   Card(
      modifier = Modifier
         .height(80.dp)
         .fillMaxWidth(),
      shape = RoundedCornerShape(percent = 10),
      onClick = { onDetail() }
   ) {
      Row(
         modifier = Modifier.padding(all = 4.dp).fillMaxSize(),
         verticalAlignment = Alignment.CenterVertically,
      ) {
         ImageRenderer(
            modifier = Modifier.weight(0.20f),
            imageVector = Icons.Default.AccountCircle,
            imagePath = imagePath,
            contentDescription = stringResource(R.string.image)
         )

         Column(
            modifier = Modifier.weight(0.70f).padding(start = 8.dp)
         ) {
            Text(
               text = "$firstName $lastName",
               style = MaterialTheme.typography.bodyLarge,
            )
            email?.let { email ->
               Text(
                  text = email,
                  style = MaterialTheme.typography.bodySmall
               )
            }
            phone?.let { phone ->
               Text(
                  text = phone,
                  style = MaterialTheme.typography.bodySmall
               )
            }
         }

         IconButton(
            modifier = Modifier.weight(0.10f),
            onClick = { onDelete() }   // Event ↑
         ) {
            Icon(
               modifier = Modifier
                  .size(32.dp)
                  .background(Color.White),
               imageVector = Icons.Filled.Close,
               contentDescription = "Card löschen"
            )
         }
      }
   }
}