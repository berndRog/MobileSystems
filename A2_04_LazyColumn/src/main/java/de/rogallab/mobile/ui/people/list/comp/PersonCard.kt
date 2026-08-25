package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun PersonCard(
   firstName: String,                  // State ↓
   lastName: String,                   // State ↓
   onDetail: () -> Unit,               // Event ↑
   onRemove: () -> Unit,               // Event ↑
   modifier: Modifier = Modifier
) {
   val tag = "<-PersonCard"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   Card(
      modifier = modifier.fillMaxWidth(),
      shape = RoundedCornerShape(percent = 10),
      onClick = {
         Alog.d(tag, "onDetail clicked")
         onDetail()
      }
   ) {
      Row(
         modifier = Modifier
            .padding(vertical = 4.dp)
            .padding(horizontal = 8.dp),
         verticalAlignment = Alignment.CenterVertically,
      ) {

         Column(
            modifier = Modifier.weight(0.85f)
         ) {
            Text(
               text = "$firstName $lastName",
               style = MaterialTheme.typography.bodyLarge,
            )

         }

         IconButton(
            modifier = Modifier.weight(0.15f),
            onClick = {
               Alog.d(tag, "onRemove clicked")
               onRemove()
            }   // Event ↑
         ) {
            Icon(
               modifier = Modifier.size(32.dp)
                  .background(Color.White),
               imageVector = Icons.Filled.Close,
               contentDescription = "Card löschen"
            )
         }
      }
   }
}