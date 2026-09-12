package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.shared.ui.images.ImageRenderer
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
   cars: List<Car> = emptyList(),
   isCarsLoading: Boolean = false,
   showCars: Boolean = false,
   onCarsRequested: () -> Unit = {},
   onDismissCars: () -> Unit = {},
   onCarClick: (String) -> Unit = {},
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

      if (!isNew) {
         OutlinedButton(
            onClick = onCarsRequested,
            enabled = !isCarsLoading,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
         ) {
            if (isCarsLoading) {
               CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
               )
            }
            else {
               Icon(
                  imageVector = Icons.Default.DirectionsCar,
                  contentDescription = null,
               )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.person_offered_cars))
         }
      }

   }

   if (showCars) {
      CarsBottomSheet(
         cars = cars,
         onDismiss = onDismissCars,
         onCarClick = onCarClick,
      )
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarsBottomSheet(
   cars: List<Car>,
   onDismiss: () -> Unit,
   onCarClick: (String) -> Unit,
) {
   ModalBottomSheet(
      onDismissRequest = onDismiss,
   ) {
      Text(
         text = stringResource(R.string.person_offered_cars),
         style = MaterialTheme.typography.titleLarge,
         modifier = Modifier.padding(horizontal = 16.dp),
      )

      if (cars.isEmpty()) {
         Text(
            text = stringResource(R.string.person_offered_cars_empty),
            modifier = Modifier.padding(16.dp),
         )
      }
      else {
         LazyColumn(
            modifier = Modifier
               .fillMaxWidth()
               .heightIn(max = 420.dp),
         ) {
            items(
               items = cars,
               key = { car -> car.id },
            ) { car ->
               ListItem(
                  modifier = Modifier.clickable {
                     onCarClick(car.id)
                  },
                  leadingContent = {
                     ImageRenderer(
                        modifier = Modifier.size(
                           width = 96.dp,
                           height = 64.dp,
                        ),
                        imageVector = Icons.Default.DirectionsCar,
                        imagePath = car.primaryImagePath,
                        contentDescription = car.displayName,
                     )
                  },
                  headlineContent = {
                     Text(text = car.displayName)
                  },
                  supportingContent = {
                     car.price?.let { price ->
                        Text(text = stringResource(R.string.car_offer_price, price))
                     }
                  },
               )
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonScreen bleibt zustandslos und enthält nur die Eingabemaske sowie die
 *   Darstellung der bereits geladenen Relationsdaten.
 * - Die Fahrzeuge werden nicht beim Öffnen des PersonScreen geladen. Erst der
 *   Klick auf "Angebotene Fahrzeuge" löst onCarsRequested aus.
 * - Das ModalBottomSheet wird erst nach erfolgreichem Laden über showCars
 *   eingeblendet. Seine LazyColumn ist vom scrollbaren Personenformular getrennt.
 * - Ein Klick auf ein Fahrzeug wird über onCarClick nach außen delegiert und
 *   kann dadurch in AppNavigation zu CarKey(carId) navigieren.
 * - TopAppBar, Loading-Anzeige und SnackbarHost liegen im PersonAdapter.
 * - UI-Aktionen werden ausschließlich über Callback-Funktionen weitergegeben.
 */
