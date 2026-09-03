package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.CameraPickerHandler
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandler
import de.rogallab.mobile.shared.ui.images.GallerySelectionMode
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   snackbarHostState: SnackbarHostState,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Observe PersonUiState
   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   // Person data
   val person = personUiState.person
   var enableSave by remember { mutableStateOf(false) }
   enableSave = person.firstName.isNotEmpty() && person.lastName.isNotEmpty()

   // Handle one-time effects separately from the persistent UI state.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   // Resolve the image error text in the UI layer for errors that originate
   // directly from the Android camera interaction.
   val imageSaveError =
      stringResource(SharedR.string.error_image_save)

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(onClick = {
                  if (enableSave)
                     viewModel.onIntent(PersonIntent.Save)
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_back))
               }
            },
            title = {
               Text(text = if (personUiState.isNew) stringResource(R.string.person_create)
               else stringResource(R.string.person_detail))
            },
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState,
            modifier = Modifier.imePadding())
      },
   ) { innerPadding ->
      // Show a loading indicator if the person data is still being loaded.
      if (personUiState.isLoading) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      } else {
         // Show person data
         val person = personUiState.person

         // The gallery handler only selects images and returns their content URIs.
         GalleryPickerHandler(
            selectionMode = GallerySelectionMode.Single,
            onImagesSelected = { sourceUris ->
               sourceUris.firstOrNull()?.let { sourceUri ->
                  viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
               }
            },
         ) { galleryActions ->

            // The camera handler prepares the target file before starting the camera.
            // After a successful capture it returns the confirmed internal file path.
            CameraPickerHandler(
               imageFileStorage = imageFileStorage,
               onPhotoStored = { viewModel.onIntent(PersonIntent.CameraImageTaken(it)) },
               onError = { viewModel.onIntent(PersonIntent.ImageFailed(imageSaveError)) },
            ) { cameraActions ->

               // Connect the current UI state and all user actions to PersonScreen.
               PersonScreen(
                  isNew = personUiState.isNew,
                  isLoading = personUiState.isLoading,
                  firstName = person.firstName,
                  onFirstNameChange = { viewModel.onIntent(PersonIntent.FirstNameChange(it)) },
                  lastName = person.lastName,
                  onLastNameChange = { viewModel.onIntent(PersonIntent.LastNameChange(it)) },
                  email = person.email,
                  onEmailChange = { viewModel.onIntent(PersonIntent.EmailChange(it)) },
                  phone = person.phone,
                  onPhoneChange = { viewModel.onIntent(PersonIntent.PhoneChange(it)) },

                  // current image path is provided to the screen for display.
                  imagePath = person.imagePath,
                  // Disable image actions while a camera file is being prepared
                  imageActionsEnabled = !cameraActions.isBusy,
                  // Gallery/camera are delegated their Activity Result handlers.
                  onSelectPhoto = galleryActions.selectFromGallery,
                  onTakePhoto = cameraActions.takePhoto,
                  // Removing an image: a physical file may be deleted.
                  onRemovePhoto = { viewModel.onIntent(PersonIntent.RemoveImage(null)) },

                  // Save/Cancel actions remain ViewModel intents.
                  onSave = { viewModel.onIntent(PersonIntent.Save) },
                  onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

                  modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding)
                     .padding(horizontal = 16.dp)
                     .verticalScroll(rememberScrollState())
                     .imePadding()
               )
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonAdapter bildet weiterhin die Verbindung zwischen PersonViewModel
 *   und dem zustandslosen PersonScreen. Er beobachtet den StateFlow, verarbeitet
 *   einmalige Effects und übersetzt Callback-Funktionen des Screens in Intents
 *   des ViewModels.
 *
 * - A3_04 erweitert diese Adapter-Aufgabe um die Anbindung an zwei getrennte
 *   Android-Mechanismen für Bilder:
 *
 *      GalleryPickerHandler
 *          Auswahl eines vorhandenen Bildes
 *
 *      CameraPickerHandler
 *          Aufnahme eines neuen Fotos
 *
 * - Beide Vorgänge werden bewusst nicht mehr in einem gemeinsamen
 *   ImagePickerHandler zusammengefasst. Dadurch werden die unterschiedlichen
 *   technischen Abläufe von Galerie und Kamera im Code deutlich sichtbar.
 *
 * - Der GalleryPickerHandler verwendet den Android Photo Picker und liefert
 *   nach einer erfolgreichen Auswahl eine Content-Uri. Er speichert die Datei
 *   selbst noch nicht im privaten App-Speicher.
 *
 * - Die Uri wird über PersonIntent.GalleryImageSelected an PersonViewModel
 *   weitergegeben. Erst dort wird IImageFileStorage verwendet, um das
 *   ausgewählte Bild in den privaten App-Speicher zu kopieren:
 *
 *      GalleryPickerHandler
 *          -> Uri
 *          -> PersonAdapter
 *          -> PersonIntent.GalleryImageSelected
 *          -> PersonViewModel
 *          -> IImageFileStorage.copyImageToAppStorage(...)
 *          -> interner Dateipfad
 *          -> ImageEditDelegate
 *          -> PersonUiState
 *
 * - Für die Kamera ist der Ablauf anders. Eine Kamera-Anwendung benötigt schon
 *   vor dem Start ein Ziel, in das sie das Foto schreiben kann. Deshalb verwendet
 *   CameraPickerHandler IImageFileStorage direkt, um zunächst eine Kamera-Datei
 *   und deren Uri vorzubereiten.
 *
 * - Nach einer erfolgreichen Aufnahme bestätigt CameraPickerHandler die Datei
 *   und liefert den internen Dateipfad über onPhotoStored zurück:
 *
 *      CameraPickerHandler
 *          -> IImageFileStorage.createCameraImageFile()
 *          -> Kamera
 *          -> IImageFileStorage.confirmCameraImageFile(...)
 *          -> interner Dateipfad
 *          -> PersonAdapter
 *          -> PersonIntent.CameraImageTaken
 *          -> PersonViewModel
 *          -> ImageEditDelegate
 *          -> PersonUiState
 *
 * - Damit liegt IImageFileStorage absichtlich an zwei unterschiedlichen Stellen
 *   im Ablauf:
 *
 *      Galerie:
 *          PersonViewModel verwendet IImageFileStorage nach der Bildauswahl.
 *
 *      Kamera:
 *          CameraPickerHandler benötigt IImageFileStorage bereits vor dem
 *          Start der Kamera.
 *
 * - ImageEditDelegate bleibt von diesen Android-spezifischen Abläufen
 *   unabhängig. Er erhält nur interne Dateipfade und verwaltet damit die
 *   Edit-Session. Er entscheidet insbesondere, welche neuen Bilder bei Cancel
 *   entfernt und welche alten Bilder erst nach erfolgreichem Save gelöscht
 *   werden dürfen.
 *
 * - PersonScreen kennt weder GalleryPickerHandler noch CameraPickerHandler,
 *   IImageFileStorage oder ImageEditDelegate. Für den Screen bestehen die
 *   Bildoperationen lediglich aus den Callback-Funktionen:
 *
 *      onSelectPhoto
 *      onTakePhoto
 *      onRemovePhoto
 *
 * - Dadurch bleibt die bekannte Aufgabenteilung erhalten:
 *
 *      PersonScreen
 *          Darstellung und Benutzerinteraktion
 *
 *      PersonAdapter
 *          State und Effects beobachten
 *          Intents erzeugen
 *          GalleryPickerHandler und CameraPickerHandler anbinden
 *
 *      PersonViewModel
 *          Anwendungslogik koordinieren
 *          Gallery-Uri in internen Dateipfad umwandeln
 *          PersonUiState aktualisieren
 *
 *      ImageEditDelegate
 *          Lebensdauer der Bilder innerhalb einer Edit-Session verwalten
 *
 *      IImageFileStorage
 *          technische Dateioperationen durchführen
 *
 * Lernziele:
 *
 * - Stateful Adapter und stateless Screen voneinander unterscheiden.
 * - State und Effects getrennt beobachten und verarbeiten.
 * - Unterschiedliche Activity-Result-Abläufe für Galerie und Kamera erkennen.
 * - GalleryPickerHandler und CameraPickerHandler nach Verantwortung trennen.
 * - Android-spezifische Picker-Logik aus PersonScreen heraushalten.
 * - UI-Ereignisse über Intents an das ViewModel weiterleiten.
 * - Technische Dateiverwaltung und Edit-Session-Verwaltung unterscheiden.
 * - Delegation zur Wiederverwendung gemeinsamer Bildlogik einsetzen.
 */