package de.rogallab.mobile.ui.people.input_detail.composables

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.copyImageToAppStorage
import de.rogallab.mobile.data.local.io.createCameraImageFile
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.common.uiText
import de.rogallab.mobile.ui.components.InputValueString
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.sanitizePhoneInput
import kotlinx.coroutines.launch
import java.io.File

/**
 * Shared editable person form used by the common create/edit screen.
 *
 * Images selected from the Photo Picker and photos taken with the camera are
 * copied to the app's private files directory. Only the resulting absolute
 * local file path is propagated through PersonIntent.ImageChanged.
 */

@Composable
fun PersonContent(
   person: Person,
   validator: PersonValidator,
   onIntent: (PersonIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PersonContent"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }


   // Bündelt die Launcher-Logik für Gallerie & Kamera
   val imagePickerHandler = rememberPersonImagePickerHandler(onIntent = onIntent)

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      InputValueString(
         value = person.firstName,
         onValueChange = {
            onIntent(PersonIntent.FirstNameChanged(it))
         },
         label = stringResource(R.string.person_field_first_name),
         leadingIcon = Icons.Default.AccountCircle,
         validate = validator::validateFirstName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.lastName,
         onValueChange = {
            onIntent(PersonIntent.LastNameChanged(it))
         },
         label = stringResource(R.string.person_field_last_name),
         leadingIcon = Icons.Default.Person,
         validate = validator::validateLastName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.email.orEmpty(),
         onValueChange = {
            onIntent(PersonIntent.EmailChanged(it))
         },
         label = stringResource(R.string.person_field_email),
         leadingIcon = Icons.Default.Email,
         validate = validator::validateEmail,
         keyboardType = KeyboardType.Email,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = person.phone.orEmpty(),
         onValueChange = { onIntent(PersonIntent.PhoneChanged(it)) },
         label = stringResource(R.string.person_field_phone),
         leadingIcon = Icons.Default.Phone,
         validate = validator::validatePhone,
         transformInput = ::sanitizePhoneInput,
         keyboardType = KeyboardType.Phone,
         imeAction = ImeAction.Done,
      )

      // Show Image, select or take photo
      PersonImageSelection(
         person = person,
         onSelectPhoto = { imagePickerHandler.openPhotoPicker() },
         onTakePhoto = { imagePickerHandler.openCamera() },
         onRemovePhoto = { onIntent(PersonIntent.ImageChanged(null)) }
      )
   }
}








/*
@Composable
fun PersonContent(
   person: Person,
   validator: PersonValidator,
   onIntent: (PersonIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val context = LocalContext.current
   val coroutineScope = rememberCoroutineScope()
   val imageSaveError = uiText(R.string.error_image_save)

   var pendingCameraImagePath by rememberSaveable {
      mutableStateOf<String?>(null)
   }

   val photoPicker = rememberLauncherForActivityResult(
      contract = PickVisualMedia(),
   ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult

      coroutineScope.launch {
         copyImageToAppStorage(
            context = context,
            sourceUri = uri,
         )
            .onSuccess { imagePath ->
               onIntent(PersonIntent.ImageChanged(imagePath))
            }
            .onFailure {
               onIntent(PersonIntent.ImageStorageFailed(imageSaveError))
            }
      }
   }

   val camera = rememberLauncherForActivityResult(
      contract = TakePicture(),
   ) { photoWasTaken ->
      val imagePath = pendingCameraImagePath
      pendingCameraImagePath = null

      when {
         photoWasTaken && imagePath != null -> {
            onIntent(PersonIntent.ImageChanged(imagePath))
         }

         imagePath != null -> {
            deleteImageFromAppStorage(imagePath)
         }
      }
   }

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

         PersonImage(
            modifier = Modifier.weight(1f),
            imagePath = person.imagePath,
            contentDescription = person.displayName
               .takeUnless(String::isBlank)
               ?.let { displayName ->
                  stringResource(
                     R.string.person_image_named,
                     displayName,
                  )
               }
               ?: stringResource(R.string.person_image),
         )

         Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
         ) {
            Button(
               onClick = {
                  photoPicker.launch(
                     PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                  )
               }
            ) {
               Text(text = stringResource(R.string.action_select_photo))
            }

            Button(
               onClick = {
                  createCameraImageFile(context)
                     .onSuccess { cameraImage ->
                        pendingCameraImagePath = cameraImage.imagePath
                        camera.launch(cameraImage.contentUri)
                     }
                     .onFailure {
                        onIntent(
                           PersonIntent.ImageStorageFailed(imageSaveError)
                        )
                     }
               },
            ) {
               Text(text = stringResource(R.string.action_take_photo))
            }

            if (!person.imagePath.isNullOrBlank()) {
               OutlinedButton(
                  onClick = {
                     onIntent(PersonIntent.ImageChanged(null))
                  },
               ) {
                  Text(text = stringResource(R.string.action_remove_photo))
               }
            }
         }
      }
   }
}

@Composable
private fun PersonImage(
   imagePath: String?,
   contentDescription: String,
   modifier: Modifier = Modifier,
) {
   Surface(
      modifier = modifier
         .fillMaxWidth()
         .height(220.dp),
      shape = RoundedCornerShape(16.dp),
   ) {
      if (imagePath.isNullOrBlank()) {
         Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Icon(
               imageVector = Icons.Default.AccountCircle,
               contentDescription = contentDescription,
               modifier = Modifier.size(120.dp),
            )
         }
      }
      else {
         AsyncImage(
            model = imagePath.toImageModel(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
         )
      }
   }
}

/** Supports new local file paths as well as older content/file URI values. */
private fun String.toImageModel(): Any =
   when {
      startsWith("content://") || startsWith("file://") -> Uri.parse(this)
      else -> File(this)
   }
*/