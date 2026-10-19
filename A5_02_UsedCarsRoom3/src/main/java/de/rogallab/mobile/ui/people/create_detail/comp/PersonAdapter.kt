package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.CameraPickerHandler
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandlerSingle
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonLoadFailure
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   bottomBar: @Composable () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   onNavigateToCar: (String) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   val person = personUiState.person
   val loadFailure = personUiState.loadFailure
   var showCars by remember { mutableStateOf(false) }

   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         PersonEffect.ShowCars -> showCars = true
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   BackHandler { viewModel.onIntent(PersonIntent.Cancel) }

   val imageSaveError = stringResource(SharedR.string.error_image_save)

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(
                  enabled = !personUiState.isSaving && !personUiState.isLoading,
                  onClick = {
                     viewModel.onIntent(if (loadFailure == null) PersonIntent.Save else PersonIntent.Cancel)
                  },
               ) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(
                        if (loadFailure == null) R.string.action_save else R.string.action_back
                     ),
                  )
               }
            },
            title = {
               Text(
                  text = if (personUiState.isNew)
                     stringResource(R.string.person_create)
                  else
                     stringResource(R.string.person_detail),
               )
            },
         )
      },
      bottomBar = bottomBar,
   ) { innerPadding ->
      if (personUiState.isLoading || personUiState.isSaving) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      }
      else if (loadFailure != null) {
         Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Text(
               text = loadFailure.message,
               style = MaterialTheme.typography.bodyLarge,
               fontWeight = FontWeight.Bold,
               modifier = Modifier.padding(bottom = 16.dp),
            )
            Button(onClick = {
               viewModel.onIntent(
                  if (loadFailure is PersonLoadFailure.NotFound) PersonIntent.Cancel
                  else PersonIntent.RetryLoad
               )
            }) {
               Text(text = stringResource(
                  if (loadFailure is PersonLoadFailure.NotFound) R.string.action_back
                  else R.string.action_retry
               ))
            }
         }
      }
      else {
         GalleryPickerHandlerSingle(
            onImageSelected = { sourceUri ->
               viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
            },
         ) { galleryActions ->

            CameraPickerHandler(
               imageFileStorage = imageFileStorage,
               onPhotoStored = {
                  viewModel.onIntent(PersonIntent.CameraImageTaken(it))
               },
               onError = {
                  viewModel.onIntent(PersonIntent.ImageFailed(imageSaveError))
               },
            ) { cameraActions ->

               PersonScreen(
                  isNew = personUiState.isNew,
                  isLoading = personUiState.isLoading,
                  isSaving = personUiState.isSaving,
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
                  imageActionsEnabled = !cameraActions.isBusy,
                  onSelectPhoto = galleryActions.selectFromGallery,
                  onTakePhoto = cameraActions.takePhoto,
                  onRemovePhoto = {
                     viewModel.onIntent(PersonIntent.RemoveImage(null))
                  },
                  cars = personUiState.cars,
                  isCarsLoading = personUiState.isCarsLoading,
                  showCars = showCars,
                  onCarsRequested = {
                     viewModel.onIntent(PersonIntent.CarsRequested)
                  },
                  onDismissCars = {
                     showCars = false
                  },
                  onCarClick = { carId ->
                     showCars = false
                     onNavigateToCar(carId)
                  },
                  onSave = { viewModel.onIntent(PersonIntent.Save) },
                  onCancel = { viewModel.onIntent(PersonIntent.Cancel) },
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding)
                     .padding(horizontal = 16.dp)
                     .verticalScroll(rememberScrollState())
                     .imePadding(),
               )
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Scaffold liegt im PersonAdapter; der einzige SnackbarHost liegt
 *   navigationweit in AppNavigation. Der Eingabescreen bleibt zustandslos.
 * - SingleGalleryPickerHandler und CameraPickerHandler bleiben ebenfalls Aufgabe des
 *   Adapters; PersonScreen erhält nur Werte und Callback-Funktionen.
 * - PersonEffect.ShowCars öffnet das Bottom Sheet erst nach erfolgreichem Laden
 *   der angebotenen Fahrzeuge. Die Sichtbarkeit bleibt lokaler UI-Zustand.
 * - Die Navigation zu einem Fahrzeug wird über onNavigateToCar an
 *   AppNavigation delegiert.
 * - Die Bottom-Navigation wird von AppNavigation bereitgestellt. Dauerhafte
 *   Ladefehler bieten RetryLoad; System-Back sendet Cancel für Bildbereinigung.
 */
