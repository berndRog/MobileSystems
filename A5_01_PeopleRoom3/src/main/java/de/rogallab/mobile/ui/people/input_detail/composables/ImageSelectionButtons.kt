package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R

@Composable
fun ImageSelectionButtons(
   modifier: Modifier = Modifier,
   imagePath: String?,
   onSelectPhoto: () -> Unit,
   onTakePhoto: () -> Unit,
   onRemovePhoto: () -> Unit
) {

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.End,
   ) {
      Button(
         modifier = Modifier
            .fillMaxWidth()
            .height(ButtonDefaults.MinHeight * 1.2f),
         onClick = onSelectPhoto
      ) {
         Text(stringResource(R.string.action_select_photo))
      }

      Button(
         modifier = Modifier
            .fillMaxWidth()
            .height(ButtonDefaults.MinHeight * 1.2f),
         onClick = onTakePhoto
      ) {
         Text(stringResource(R.string.action_take_photo))
      }

      if (!imagePath.isNullOrBlank()) {
         OutlinedButton(
            modifier = Modifier
               .fillMaxWidth()
               .height(ButtonDefaults.MinHeight * 1.2f),
            onClick = onRemovePhoto
         ) {
            Text(stringResource(R.string.action_remove_photo))
         }
      }
   }

}