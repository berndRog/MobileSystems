package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.CameraPickerHandler
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandlerSingle
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonLoadFailure
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import org.koin.compose.koinInject
import de.rogallab.mobile.shared.R as SharedR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
   modifier: Modifier = Modifier,
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Observe PersonUiState
   val personUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   // Person data
   val person = personUiState.person
   val loadFailure = personUiState.loadFailure

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

   val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

   // Keep result handlers composed during loading and saving.
   GalleryPickerHandlerSingle(
      onImageSelected = { viewModel.onIntent(PersonIntent.GalleryImageSelected(it)) },
   ) { galleryActions ->
      CameraPickerHandler(
         imageFileStorage = imageFileStorage,
         onPhotoStored = { viewModel.onIntent(PersonIntent.CameraImageTaken(it)) },
         onError = { viewModel.onIntent(PersonIntent.ImageFailed(imageSaveError)) },
      ) { cameraActions ->
         val imageBusy = personUiState.isEditingImages || cameraActions.isBusy
         // Consume Back until the current operation can finish safely.
         NavigationBackHandler(
            state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
            isBackEnabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
            onBackCompleted = {
               if (!imageBusy) viewModel.onIntent(PersonIntent.Cancel)
            },
         )

         Scaffold(
            modifier = modifier,
            topBar = {
               TopAppBar(
                  navigationIcon = {
                     IconButton(enabled = !personUiState.isSaving && !personUiState.isLoading && !imageBusy, onClick = {
                        viewModel.onIntent(if (loadFailure == null) PersonIntent.Save else PersonIntent.Cancel)
                     }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                           contentDescription = stringResource(if (loadFailure == null) R.string.action_save else R.string.action_back))
                     }
                  },
                  title = {
                     Text(text = stringResource( if (personUiState.isNew) R.string.person_create
                     else R.string.person_detail ))
                  },
               )
            },
         ) { innerPadding ->
            // Show initial loading before the form is available.
            if (personUiState.isLoading) {
               Box(
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding),
                  contentAlignment = Alignment.TopCenter,
               ) {
                  CircularProgressIndicator(modifier = Modifier.size(64.dp))
               }
            }
            // load failure screen
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
                  Button(enabled = !imageBusy, onClick = {
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
            // Show person form
            else {
               Box(
                  modifier = Modifier
                     .fillMaxSize().padding(innerPadding),
               ) {

                  // Connect the current UI state and all user actions to PersonScreen.
                  PersonScreen(
                     isNew = personUiState.isNew,
                     isLoading = personUiState.isLoading,
                     isSaving = personUiState.isSaving,
                     isEditingImages = imageBusy,
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
                     imageActionsEnabled = !personUiState.isSaving && !imageBusy,
                     // Gallery is delegated to its Activity Result handler.
                     onSelectPhoto = galleryActions.selectFromGallery,
                     // Camera is delegated to its Activity Result handler.
                     onTakePhoto = cameraActions.takePhoto,
                     // Removing an image: a physical file may be deleted.
                     onRemovePhoto = { viewModel.onIntent(PersonIntent.RemoveImage(null)) },

                     // Save/Cancel actions remain ViewModel intents.
                     onSave = { viewModel.onIntent(PersonIntent.Save) },
                     onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

                     modifier = Modifier
                        .fillMaxSize().padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()).imePadding()
                  )

                  if (personUiState.isSaving) {
                     Box(
                        modifier = Modifier
                           .matchParentSize()
                           .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                           .clickable(
                              interactionSource = remember { MutableInteractionSource() },
                              indication = null,
                              onClick = {},
                           ),
                        contentAlignment = Alignment.TopCenter,
                     ) {
                        CircularProgressIndicator(
                           modifier = Modifier.padding(top = 24.dp).size(64.dp),
                        )
                     }
                  }
               }
            }
         }
      }
   }
}
