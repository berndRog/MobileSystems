package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

   onNavigateBack: () -> Unit = {},
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
            IconButton(onClick = onNavigateBack) {
               Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back))
            }
         },
         title = {
            Text(text = if (isNew) stringResource(R.string.person_create)
                        else stringResource(R.string.person_detail))
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

      // The form itself remains stateless. All changes are sent back through
      // callback functions provided by PersonAdapter.
      Column(
         modifier = Modifier.padding(horizontal = 16.dp)
      ) {
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

         // ImageSelection only renders the current image and the available
         // image actions. Gallery and camera handling are provided by the
         // PersonAdapter through the callback functions.
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
               onClick = onCancel
            ) {
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
}

/*
 * Didaktik und Lernziele
 *
 * - PersonScreen bleibt auch nach der Erweiterung um die Bildbearbeitung ein
 *   zustandsloses Composable. Der Screen erhält ausschließlich Werte und
 *   Callback-Funktionen und kennt weder ViewModel noch Repository,
 *   Navigation oder SnackbarController.
 *
 * - A3_04 ergänzt die bisherige Personenbearbeitung um die Anzeige und
 *   Bearbeitung eines Personenbildes. ImageSelection übernimmt dabei nur noch
 *   die Darstellung des aktuellen Bildes sowie die Schaltflächen für
 *   Galerie, Kamera und Entfernen.
 *
 * - Die technische Anbindung an Android ist bewusst nicht Bestandteil von
 *   PersonScreen. GalleryPickerHandler und CameraPickerHandler werden im
 *   PersonAdapter verwendet. Der Screen erhält lediglich die delegierten
 *   Funktionen onSelectPhoto und onTakePhoto.
 *
 * - Damit bleiben die Aufgaben klar getrennt:
 *
 *      PersonScreen
 *          Darstellung und Benutzerinteraktion
 *
 *      PersonAdapter
 *          Verbindung zum ViewModel
 *          Verwendung der beiden Picker-Handler
 *
 *      GalleryPickerHandler
 *          Auswahl eines vorhandenen Bildes
 *          Rückgabe einer Content-Uri
 *
 *      CameraPickerHandler
 *          Aufnahme eines neuen Fotos
 *          Verwaltung der dafür benötigten Kamera-Datei
 *
 * - Nach einer Galerieauswahl erhält das ViewModel zunächst eine Uri.
 *   Dort wird das ausgewählte Bild über IImageFileStorage in den privaten
 *   App-Speicher kopiert. Erst der dadurch entstandene interne Dateipfad
 *   wird anschließend über ImageEditDelegate in die laufende Edit-Session
 *   übernommen.
 *
 * - Bei einer Kameraaufnahme wird die Zieldatei bereits vor dem Start der
 *   Kamera vorbereitet. Nach einer erfolgreichen Aufnahme liefert der
 *   CameraPickerHandler den bestätigten internen Dateipfad zurück.
 *
 * - Für PersonScreen spielt dieser technische Unterschied keine Rolle.
 *   Galerie und Kamera erscheinen hier lediglich als zwei Callback-Funktionen.
 *   Dadurch bleibt die UI unabhängig von ActivityResultContracts,
 *   Content-Uris und Dateiverwaltung.
 *
 * - Auch das Entfernen eines Bildes führt nicht direkt zu einer Dateioperation.
 *   PersonScreen meldet lediglich onRemovePhoto. Die weitere Verarbeitung
 *   erfolgt über PersonAdapter, PersonViewModel und ImageEditDelegate.
 *
 * Lernziele:
 *
 * - Stateful Adapter und stateless Screen klar voneinander trennen.
 * - Android-spezifische Activity-Result-Logik aus dem Screen heraushalten.
 * - GalleryPickerHandler und CameraPickerHandler für unterschiedliche
 *   technische Abläufe verwenden.
 * - UI-Aktionen über Callback-Funktionen an den Adapter delegieren.
 * - Bildanzeige und technische Bildauswahl als getrennte Aufgaben verstehen.
 * - Die bestehende MVI-/UDF-Struktur auch für Bildoperationen beibehalten.
 */