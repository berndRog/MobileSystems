package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
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

   onBack: () -> Unit = {},
   onSave: () -> Unit = {},
   onCancel: () -> Unit = {},

   modifier: Modifier = Modifier,
   validator: PersonValidator = koinInject(),
) {
   val tag = "<-PersonScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Saving is enabled only when the mandatory name fields contain values.
   val enableSave = firstName.isNotEmpty() && lastName.isNotEmpty()

   Column(
      modifier = modifier
   ) {

      // The TopAppBar delegates back navigation to the caller.
      TopAppBar(
         windowInsets = WindowInsets(0),
         navigationIcon = {
            IconButton(
               onClick = onBack
            ) {
               Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = stringResource(R.string.action_back),
               )
            }
         },
         title = {
            Text(
               text =
                  if (isNew) stringResource(R.string.person_create)
                  else stringResource(R.string.person_detail)
            )
         },
      )

      // While an existing person is loaded, the form is not displayed.
      if (isLoading) {
         Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            CircularProgressIndicator()
         }
         return@Column
      }

      // The form remains stateless. All changes are sent back through
      // callback functions supplied by PersonAdapter.
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

      // ImageSelection renders the current image and the image buttons only.
      // Gallery and camera Activity Result handling live in PersonAdapter.
      ImageSelection(
         fullName = "$firstName $lastName".trim(),
         imagePath = imagePath,
         imageActionsEnabled = imageActionsEnabled,
         onSelectPhoto = onSelectPhoto,
         onTakePhoto = onTakePhoto,
         onRemovePhoto = onRemovePhoto,
      )

      // Save and Cancel are also delegated to the caller.
      Row(
         modifier = Modifier.fillMaxWidth(),
         horizontalArrangement = Arrangement.spacedBy(
            40.dp,
            Alignment.CenterHorizontally,
         ),
      ) {
         OutlinedButton(
            onClick = onCancel,
         ) {
            Text(
               text = stringResource(R.string.action_cancel)
            )
         }

         Button(
            onClick = onSave,
            enabled = enableSave,
         ) {
            Text(
               text = stringResource(R.string.action_save)
            )
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_05 übernimmt den PersonScreen aus A3_04. Die Swipe-Gesten werden im
 *   PeopleListScreen ergänzt und verändern den Aufbau des Personeneditors nicht.
 *
 * - PersonScreen bleibt ein zustandsloses Composable. Er erhält Werte und
 *   Callback-Funktionen und kennt weder ViewModel noch Repository, Navigation,
 *   SnackbarController oder Android ActivityResultContracts.
 *
 * - ImageSelection übernimmt nur die Darstellung des aktuellen Bildes sowie
 *   die Schaltflächen für Galerie, Kamera und Entfernen. Die technische
 *   Bildauswahl gehört nicht mehr zu ImageSelection.
 *
 * - GalleryPickerHandler und CameraPickerHandler werden im PersonAdapter
 *   verwendet. PersonScreen erhält lediglich onSelectPhoto und onTakePhoto.
 *   Dadurch ist für den Screen unerheblich, dass Galerie und Kamera technisch
 *   völlig unterschiedliche Abläufe besitzen.
 *
 * - Nach einer Galerieauswahl wird zunächst eine Content-Uri an das ViewModel
 *   weitergegeben. Dort kopiert IImageFileStorage das Bild in den privaten
 *   App-Speicher. Eine Kameraaufnahme liefert dagegen bereits einen bestätigten
 *   internen Dateipfad.
 *
 * - Auch onRemovePhoto löst im Screen keine Dateioperation aus. Die weitere
 *   Verarbeitung erfolgt über PersonAdapter, PersonViewModel und IImageEdit.
 *
 * Lernziele:
 *
 * - Stateful Adapter und stateless Screen klar voneinander trennen.
 * - Android-spezifische Activity-Result-Logik aus dem Screen heraushalten.
 * - GalleryPickerHandler und CameraPickerHandler getrennt verwenden.
 * - Bildanzeige und technische Bildauswahl als unterschiedliche Aufgaben sehen.
 * - UI-Aktionen ausschließlich über Callback-Funktionen delegieren.
 * - Den bestehenden UDF-/MVI-Datenfluss auch für Bildoperationen beibehalten.
 */
