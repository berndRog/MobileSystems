package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.shared.ui.images.ImageSelection
import de.rogallab.mobile.ui.people.PersonValidator
import org.koin.compose.koinInject

@Composable
fun PersonScreen(
   isNew: Boolean,
   isLoading: Boolean,
   firstName: String = "",
   onFirstNameChange: (String) -> Unit = {},
   lastName: String = "",
   onLastNameChange: (String) -> Unit = {},
   email: String? = "",
   onEmailChange: (String) -> Unit = {},
   phone: String? = "",
   onPhoneChange: (String) -> Unit = {},
   imagePath: String? = null,
   imageActionsEnabled: Boolean = true,
   onSelectPhoto: () -> Unit = {},
   onTakePhoto: () -> Unit = {},
   onRemovePhoto: () -> Unit = {},
   onSave: () -> Unit = {},
   onCancel: () -> Unit = {},
   modifier: Modifier = Modifier,
   validator: PersonValidator = koinInject(),
) {
   val tag = "<-PersonScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val enableSave = firstName.isNotEmpty() && lastName.isNotEmpty()

   Column(modifier = modifier) {
      InputValueString(
         value = firstName,
         onValueChange = onFirstNameChange,
         label = stringResource(R.string.firstname),
         leadingIcon = Icons.Default.AccountCircle,
         validate = validator::validateFirstName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )
      InputValueString(
         value = lastName,
         onValueChange = onLastNameChange,
         label = stringResource(R.string.lastname),
         leadingIcon = Icons.Default.Person,
         validate = validator::validateLastName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )
      InputValueString(
         value = email.orEmpty(),
         onValueChange = onEmailChange,
         label = stringResource(R.string.email),
         leadingIcon = Icons.Default.Email,
         validate = validator::validateEmail,
         keyboardType = KeyboardType.Email,
         imeAction = ImeAction.Next,
      )
      InputValueString(
         value = phone.orEmpty(),
         onValueChange = onPhoneChange,
         label = stringResource(R.string.phone),
         leadingIcon = Icons.Default.Phone,
         validate = validator::validatePhone,
         keyboardType = KeyboardType.Phone,
         imeAction = ImeAction.Done,
      )

      ImageSelection(
         fullName = "$firstName $lastName".trim(),
         imagePath = imagePath,
         imageActionsEnabled = imageActionsEnabled,
         onSelectPhoto = onSelectPhoto,
         onTakePhoto = onTakePhoto,
         onRemovePhoto = onRemovePhoto,
      )

      Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(
            40.dp,
            Alignment.CenterHorizontally,
         ),
      ) {
         OutlinedButton(onClick = onCancel) {
            Text(text = stringResource(R.string.action_cancel))
         }
         Button(
            onClick = onSave,
            enabled = enableSave,
         ) {
            Text(text = stringResource(R.string.action_save))
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonScreen bleibt zustandslos und enthält nur die Eingabemaske.
 * - TopAppBar, Loading-Anzeige und SnackbarHost liegen im PersonAdapter.
 * - UI-Aktionen werden ausschließlich über Callback-Funktionen weitergegeben.
 */
