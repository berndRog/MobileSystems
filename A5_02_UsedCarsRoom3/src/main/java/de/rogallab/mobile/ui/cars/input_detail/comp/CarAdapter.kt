package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.CameraPickerHandler
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandler
import de.rogallab.mobile.shared.ui.images.GallerySelectionMode
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.input_detail.CarIntent
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
import de.rogallab.mobile.ui.people.create_detail.BackReason
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarAdapter(
   viewModel: CarViewModel,
   snackbarHostState: SnackbarHostState,
   bottomBar: @Composable () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: CarValidator = koinInject(),
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val carUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is CarEffect.ShowMessage -> onMessage(effect.message)
         is CarEffect.ShowError -> onError(effect.message)
         is CarEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }

   val imageCount = carUiState.car?.imagePaths?.size ?: 0
   val remainingSlots = (MAX_CAR_IMAGE_COUNT - imageCount).coerceAtLeast(1)
   val canAddMoreImages = imageCount < MAX_CAR_IMAGE_COUNT
   val imageSaveError = stringResource(SharedR.string.error_image_save)

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            title = {
               Text(
                  stringResource(
                     if (carUiState.isNew) R.string.car_create_title
                     else R.string.car_edit_title,
                  )
               )
            },
            navigationIcon = {
               IconButton(onClick = { viewModel.onIntent(CarIntent.Save) }) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_save),
                  )
               }
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
      if (carUiState.isLoading) {
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
            selectionMode = if (remainingSlots == 1)
               GallerySelectionMode.Single
            else
               GallerySelectionMode.Multiple,
            maxSelectionCount = remainingSlots,
            onImagesSelected = { sourceUris ->
               viewModel.onIntent(CarIntent.GalleryImagesSelected(sourceUris))
            },
         ) { galleryActions ->
            CameraPickerHandler(
               imageFileStorage = imageFileStorage,
               onPhotoStored = { imagePath ->
                  viewModel.onIntent(CarIntent.CameraImageTaken(imagePath))
               },
               onError = {
                  viewModel.onIntent(CarIntent.ImageFailed(imageSaveError))
               },
            ) { cameraActions ->
               CarScreen(
                  carUiState = carUiState,
                  validator = validator,
                  onSelectImages = {
                     if (canAddMoreImages) galleryActions.selectFromGallery()
                  },
                  onTakePhoto = {
                     if (canAddMoreImages) cameraActions.takePhoto()
                  },
                  onIntent = viewModel::onIntent,
                  modifier = Modifier
                     .fillMaxSize()
                     .padding(innerPadding),
               )
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - CarAdapter bindet die Shared-Picker an das Fahrzeug-Feature und enthält
 *   zusätzlich den Scaffold der Detailansicht.
 * - TopAppBar, Loading und SnackbarHost liegen damit außerhalb von CarScreen.
 * - CarScreen bleibt für Eingabefelder, Bildvorschau und Benutzeraktionen
 *   zuständig und erhält diese Abhängigkeiten nur über Parameter und Callbacks.
 */
