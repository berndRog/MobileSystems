package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
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
   bottomBar: @Composable () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   val person = personUiState.person
   val enableSave = person.firstName.isNotEmpty() && person.lastName.isNotEmpty()

   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   val imageSaveError = stringResource(SharedR.string.error_image_save)

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(
                  onClick = {
                     if (enableSave) viewModel.onIntent(PersonIntent.Save)
                  },
               ) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_back),
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
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding(),
         )
      },
   ) { innerPadding ->
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
      else {
         GalleryPickerHandler(
            selectionMode = GallerySelectionMode.Single,
            onImagesSelected = { sourceUris ->
               sourceUris.firstOrNull()?.let { sourceUri ->
                  viewModel.onIntent(PersonIntent.GalleryImageSelected(sourceUri))
               }
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
 * - Der Scaffold liegt wie in A5_01 im PersonAdapter. Damit bleiben TopAppBar,
 *   SnackbarHost und die zustandslose Eingabemaske klar getrennt.
 * - GalleryPickerHandler und CameraPickerHandler bleiben ebenfalls Aufgabe des
 *   Adapters; PersonScreen erhält nur Werte und Callback-Funktionen.
 * - Der gemeinsame SnackbarHostState und die Bottom-Navigation werden von
 *   AppNavigation bereitgestellt und in diesem Scaffold verwendet.
 */
