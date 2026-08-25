package de.rogallab.mobile.ui.people.create_detail.comp

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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.ImageRenderer
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.ui.people.PersonValidator
import org.koin.compose.koinInject

@Composable  // MVI pattern
fun PersonScreen(
   firstName: String = "",
   onFirstNameChange: (String) -> Unit = {},

   lastName: String = "",
   onLastNameChange: (String) -> Unit = {},

   email: String? = "",
   onEmailChange: (String) -> Unit = {},

   phone: String? = "",
   onPhoneChange: (String) -> Unit = {},

   imagePath: String? = null,

   onSave: () -> Unit = {},
   onCancel: () -> Unit = {},

   modifier: Modifier = Modifier,
   validator: PersonValidator = koinInject(),
) {
   val tag = "<-PersonScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   var enableSave by remember { mutableStateOf(false) }
   enableSave = firstName.isNotEmpty() && lastName.isNotEmpty()

   Column(
      modifier = modifier
   ) {
      InputValueString(
         value = firstName,
         onValueChange = { onFirstNameChange(it) },
         label = stringResource(R.string.firstname),
         leadingIcon = Icons.Default.AccountCircle,
         validate = validator::validateFirstName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )
      InputValueString(
         value = lastName,
         onValueChange = { onLastNameChange(it) },
         label = stringResource(R.string.lastname),
         leadingIcon = Icons.Default.Person,
         validate = validator::validateLastName,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = email.orEmpty(),
         onValueChange = { onEmailChange(it) },
         label = stringResource(R.string.email),
         leadingIcon = Icons.Default.Email,
         validate = validator::validateEmail,
         keyboardType = KeyboardType.Text,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = phone.orEmpty(),
         onValueChange = { onPhoneChange(it) },
         label = stringResource(R.string.phone),
         leadingIcon = Icons.Default.Phone,
         validate = validator::validatePhone,
         keyboardType = KeyboardType.Phone,
         imeAction = ImeAction.Done,
      )

      ImageRenderer(
         modifier = Modifier
            .padding(vertical = 16.dp)
            .height(220.dp)
            .fillMaxWidth(),
         imageVector = Icons.Default.AccountCircle,
         imagePath = imagePath,
         contentDescription = "$firstName $lastName"
      )

      Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(
            40.dp, Alignment.CenterHorizontally),
      ) {
         OutlinedButton(
            onClick = { onCancel() }, //onIntent(PersonIntent.Cancel)
         ) {
            Text(text = stringResource(R.string.action_cancel))
         }

         Button(
            onClick = { onSave() }, //onIntent(PersonIntent.Save)
            enabled = enableSave,
         ) {
            Text(text = stringResource(R.string.action_save))
         }
      }
   }
}
