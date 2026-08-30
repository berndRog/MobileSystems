package de.rogallab.mobile.ui.people.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.uiText
import de.rogallab.mobile.ui.composables.ImageRenderer
import de.rogallab.mobile.ui.composables.ImageSelectionButtons
import de.rogallab.mobile.ui.composables.InputValueString
import de.rogallab.mobile.ui.composables.rememberImagePickerHandler
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.sanitizePhoneInput

// Shared editable person form used by Create and Edit.
//
// Photo Picker and camera images are copied to the private app directory.
// Only the resulting local path is forwarded through PersonIntent.ImageChanged.

private const val TAG = "<-PersonContent"

@Composable
fun PersonContent(
   person: Person,
   validator: PersonValidator,
   onIntent: (PersonIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   val personImagePicker = rememberImagePickerHandler(
      maxSelection = 1,
      onImagesSelected = { paths -> onIntent(PersonIntent.ImageChanged(paths.firstOrNull())) },
      onStorageFailed = { msg -> onIntent(PersonIntent.ImageStorageFailed(msg)) }
   )

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {

      InputValueString(
         value = person.firstName,
         onValueChange = { value ->
            onIntent(PersonIntent.FirstNameChanged(value))
         },
         label = stringResource(R.string.person_field_first_name),
         leadingIcon = Icons.Default.AccountCircle,
         validate = validator::validateFirstName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.lastName,
         onValueChange = { value ->
            onIntent(PersonIntent.LastNameChanged(value))
         },
         label = stringResource(R.string.person_field_last_name),
         leadingIcon = Icons.Default.Person,
         validate = validator::validateLastName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.email.orEmpty(),
         onValueChange = { value ->
            onIntent(PersonIntent.EmailChanged(value))
         },
         label = stringResource(R.string.person_field_email),
         leadingIcon = Icons.Default.Email,
         validate = validator::validateEmail,
         keyboardType = KeyboardType.Email,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.phone.orEmpty(),
         onValueChange = { value ->
            onIntent(PersonIntent.PhoneChanged(value))
         },
         label = stringResource(R.string.person_field_phone),
         leadingIcon = Icons.Default.Phone,
         validate = validator::validatePhone,
         transformInput = ::sanitizePhoneInput,
         keyboardType = KeyboardType.Phone,
         imeAction = ImeAction.Done,
      )

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
            imagePath = person.imagePath,
            contentDescription = person.displayName
         )

         Column(
            modifier = modifier
               .weight(1f)
               .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
         ) {
            ImageSelectionButtons(
               modifier = Modifier.weight(1f),
               imagePath = person.imagePath,
               onSelectPhoto = { personImagePicker.openGalleryPicker() },
               onTakePhoto = { personImagePicker.openCamera() },
               onRemovePhoto = { onIntent(PersonIntent.ImageChanged(null)) }
            )
         }
      }
   }
}
