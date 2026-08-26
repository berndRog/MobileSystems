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
import de.rogallab.mobile.shared.R as SharedR

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

   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onBack(personEffect.reason)
      }
   }

   val imageSaveError = stringResource(SharedR.string.error_image_save)
   val person = personUiState.person

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
         onPhotoStored = { imagePath ->
            viewModel.onIntent(PersonIntent.ImagePathChange(imagePath))
         },
         onError = {
            viewModel.onIntent(PersonIntent.ImageStorageFailed(imageSaveError))
         },
      ) { cameraActions ->
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
            imagePath = person.imagePath,
            imageActionsEnabled = !cameraActions.isBusy,
            onSelectPhoto = galleryActions.selectFromGallery,
            onTakePhoto = cameraActions.takePhoto,
            onRemovePhoto = { viewModel.onIntent(PersonIntent.ImagePathChange(null)) },
            onBack = { viewModel.onIntent(PersonIntent.Cancel) },
            onSave = { viewModel.onIntent(PersonIntent.Save) },
            onCancel = { viewModel.onIntent(PersonIntent.Cancel) },
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
