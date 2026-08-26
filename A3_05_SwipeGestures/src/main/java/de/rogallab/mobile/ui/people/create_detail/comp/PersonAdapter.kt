package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onBack: (BackReason) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Observe the persistent UI state of the person editor.
   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   // Handle one-time effects separately from the persistent UI state.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onBack(personEffect.reason)
      }
   }

   // Resolve the camera-storage error text in the UI layer because this error
   // originates directly from the Android camera interaction.
   val imageSaveError =
      stringResource(SharedR.string.error_image_save)

   val person = personUiState.person

   // The gallery handler selects images only and returns Content-URIs.
   // Copying the selected gallery image into private app storage is handled
   // later by PersonViewModel.
   GalleryPickerHandler(
      selectionMode = GallerySelectionMode.Single,
      onImagesSelected = { sourceUris ->
         sourceUris.firstOrNull()?.let { sourceUri ->
            viewModel.onIntent(
               PersonIntent.GalleryImageSelected(sourceUri)
            )
         }
      },
   ) { galleryActions ->

      // The camera handler must prepare a target file before the external
      // camera application can be started. A successful capture returns the
      // confirmed internal file path.
      CameraPickerHandler(
         imageFileStorage = imageFileStorage,
         onPhotoStored = { imagePath ->
            viewModel.onIntent(
               PersonIntent.ImagePathChange(imagePath)
            )
         },
         onError = {
            viewModel.onIntent(
               PersonIntent.ImageStorageFailed(imageSaveError)
            )
         },
      ) { cameraActions ->

         // Connect State and user actions to the stateless PersonScreen.
         PersonScreen(
            isNew = personUiState.isNew,
            isLoading = personUiState.isLoading,

            firstName = person.firstName,
            onFirstNameChange = {
               viewModel.onIntent(PersonIntent.FirstNameChange(it))
            },

            lastName = person.lastName,
            onLastNameChange = {
               viewModel.onIntent(PersonIntent.LastNameChange(it))
            },

            email = person.email,
            onEmailChange = {
               viewModel.onIntent(PersonIntent.EmailChange(it))
            },

            phone = person.phone,
            onPhoneChange = {
               viewModel.onIntent(PersonIntent.PhoneChange(it))
            },

            imagePath = person.imagePath,

            // Disable image actions while a camera operation is active to
            // avoid overlapping file preparation or confirmation requests.
            imageActionsEnabled = !cameraActions.isBusy,

            // Gallery and camera actions are delegated directly to the two
            // specialized Activity Result handlers.
            onSelectPhoto = galleryActions.selectFromGallery,
            onTakePhoto = cameraActions.takePhoto,

            // Removing an image only changes the current edit-session selection.
            // IImageEdit decides when the physical file may actually be deleted.
            onRemovePhoto = {
               viewModel.onIntent(PersonIntent.ImagePathChange(null))
            },

            // Navigation and Save/Cancel remain ViewModel intents.
            onBack = {
               viewModel.onIntent(PersonIntent.Cancel)
            },
            onSave = {
               viewModel.onIntent(PersonIntent.Save)
            },
            onCancel = {
               viewModel.onIntent(PersonIntent.Cancel)
            },

            modifier = modifier
               .fillMaxSize()
               .verticalScroll(rememberScrollState())
               .imePadding()
               .padding(horizontal = 16.dp)
               .fillMaxWidth(),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_05 übernimmt die Bildbearbeitung aus A3_04. Die neuen Swipe-Gesten
 *   betreffen den PeopleListScreen; die Bearbeitung einer einzelnen Person
 *   verwendet weiterhin PersonAdapter und PersonScreen.
 *
 * - PersonAdapter verbindet den beobachteten PersonUiState und die einmaligen
 *   PersonEffects mit dem zustandslosen PersonScreen. UI-Callbacks werden in
 *   PersonIntent-Objekte übersetzt und an PersonViewModel weitergegeben.
 *
 * - Galerie und Kamera werden bewusst durch zwei getrennte Handler angebunden:
 *
 *      GalleryPickerHandler
 *          -> vorhandenes Bild auswählen
 *          -> Content-Uri liefern
 *
 *      CameraPickerHandler
 *          -> Kamera-Zieldatei vorbereiten
 *          -> Foto aufnehmen
 *          -> internen Dateipfad bestätigen
 *
 * - Der GalleryPickerHandler speichert die ausgewählte Datei noch nicht.
 *   Die Content-Uri wird über GalleryImageSelected an das ViewModel gegeben.
 *   Dort kopiert IImageFileStorage das Bild in den privaten App-Speicher.
 *
 * - CameraPickerHandler benötigt IImageFileStorage bereits vor dem Start der
 *   Kamera, weil ActivityResultContracts.TakePicture eine Ziel-Uri erwartet.
 *   Nach erfolgreicher Aufnahme erhält der Adapter einen bestätigten internen
 *   Dateipfad und sendet ImagePathChange an das ViewModel.
 *
 * - PersonScreen kennt weder die Picker-Handler noch IImageFileStorage oder
 *   IImageEdit. Für den Screen existieren nur onSelectPhoto, onTakePhoto und
 *   onRemovePhoto. Dadurch bleibt der Screen Android-unabhängig und stateless.
 *
 * - Die Bild-Lebensdauer innerhalb einer Bearbeitung bleibt Aufgabe von
 *   IImageEdit/ImageEditDelegate. Der Adapter löst deshalb beim Entfernen keine
 *   direkte Dateioperation aus.
 *
 * Lernziele:
 *
 * - Stateful Adapter und stateless Screen voneinander trennen.
 * - State und Effects getrennt verarbeiten.
 * - Gallery- und Camera-Activity-Result-Abläufe unterscheiden.
 * - Android-spezifische Picker-Logik aus PersonScreen heraushalten.
 * - UI-Ereignisse über Intents an das ViewModel weiterleiten.
 * - Dateispeicherung und Edit-Session-Verwaltung als getrennte Aufgaben sehen.
 */
