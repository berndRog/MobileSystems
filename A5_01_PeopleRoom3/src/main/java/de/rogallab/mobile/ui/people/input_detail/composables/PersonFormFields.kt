package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.components.InputValueString
import de.rogallab.mobile.ui.people.input_detail.PersonIntent
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.sanitizePhoneInput

@Composable
fun PersonFormFields(
   person: Person,
   validator: PersonValidator,
   onIntent: (PersonIntent) -> Unit,
) {
   val tag = "<-PersonFormFields"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }

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
}