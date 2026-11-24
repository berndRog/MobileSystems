package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.CameraPickerHandler
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandlerMultiple
import de.rogallab.mobile.shared.ui.images.GalleryPickerHandlerSingle
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.input_detail.CarLoadFailure
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
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: CarValidator = koinInject(),
   imageFileStorage: IImageFileStorage = koinInject(),
) {
   val carUiState by viewModel.stateFlow.collectAsStateWithLifecycle()
   val loadFailure = carUiState.loadFailure
   val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is CarEffect.ShowMessage -> onMessage(effect.message)
         is CarEffect.ShowError -> onError(effect.message)
         is CarEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }

   val imageCount = carUiState.form.imagePaths.size
   val remainingSlots = (MAX_CAR_IMAGE_COUNT - imageCount).coerceAtLeast(1)
   val canAddMoreImages = imageCount < MAX_CAR_IMAGE_COUNT
   val imageSaveError = stringResource(SharedR.string.error_image_save)

   // Keep launchers composed, including during Save and image processing.
   GalleryPickerHandlerSingle(
      onImageSelected = { viewModel.onIntent(CarIntent.GalleryImagesSelected(listOf(it))) },
   ) { singleGalleryActions ->
      GalleryPickerHandlerMultiple(
         maxSelectionCount = remainingSlots.coerceAtLeast(2),
         onImagesSelected = { viewModel.onIntent(CarIntent.GalleryImagesSelected(it)) },
      ) { multipleGalleryActions ->
         CameraPickerHandler(
            imageFileStorage = imageFileStorage,
            onPhotoStored = { viewModel.onIntent(CarIntent.CameraImageTaken(it)) },
            onError = { viewModel.onIntent(CarIntent.ImageFailed(imageSaveError)) },
         ) { cameraActions ->
            val imageBusy = carUiState.isEditingImages || cameraActions.isBusy

            // Consume Back while busy; disabling the handler would let NavDisplay pop.
            NavigationBackHandler(
               state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
               isBackEnabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
               onBackCompleted = {
                  if (!imageBusy) viewModel.onIntent(CarIntent.Cancel)
               },
            )

            Scaffold(
               modifier = Modifier.fillMaxSize(),
               topBar = {
                  TopAppBar(
                     title = {
                        Text(stringResource(
                           if (carUiState.isNew) R.string.car_create_title else R.string.car_edit_title
                        ))
                     },
                     navigationIcon = {
                        IconButton(
                           enabled = !carUiState.isLoading && !carUiState.isSaving && !imageBusy,
                           onClick = {
                              if (!imageBusy) {
                                 viewModel.onIntent(
                                    if (loadFailure == null) CarIntent.Save else CarIntent.Cancel
                                 )
                              }
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
                  )
               },
            ) { innerPadding ->
               if (carUiState.isLoading) {
                  Box(
                     modifier = Modifier.fillMaxSize().padding(innerPadding),
                     contentAlignment = Alignment.TopCenter,
                  ) {
                     CircularProgressIndicator(modifier = Modifier.size(64.dp))
                  }
               } else if (loadFailure != null) {
                  Column(
                     modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                     verticalArrangement = Arrangement.Center,
                     horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                     Text(text = loadFailure.message)
                     Button(
                        enabled = !imageBusy,
                        onClick = {
                           viewModel.onIntent(
                              if (loadFailure is CarLoadFailure.NotFound) CarIntent.Cancel
                              else CarIntent.RetryLoad
                           )
                        },
                     ) {
                        Text(stringResource(
                           if (loadFailure is CarLoadFailure.NotFound) R.string.action_back
                           else R.string.action_retry
                        ))
                     }
                  }
               } else {
                  CarScreen(
                     carUiState = carUiState,
                     validator = validator,
                     isCameraBusy = cameraActions.isBusy,
                     onSelectImages = {
                        if (!imageBusy && canAddMoreImages) {
                           if (remainingSlots == 1) singleGalleryActions.selectFromGallery()
                           else multipleGalleryActions.selectFromGallery()
                        }
                     },
                     onTakePhoto = {
                        if (!imageBusy && canAddMoreImages) cameraActions.takePhoto()
                     },
                     onIntent = viewModel::onIntent,
                     modifier = Modifier.fillMaxSize().padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()).imePadding(),
                  )
               }
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - CarAdapter bindet beide Galerie-Handler abhängig von den freien
 *   Bildplätzen an: einer für einen Platz, der andere für mehrere Plätze.
 * - CarAdapter bindet die Shared-Picker an das Fahrzeug-Feature und enthält
 *   zusätzlich den Scaffold der Detailansicht.
 * - TopAppBar und Loading liegen im Adapter; der SnackbarHost liegt
 *   navigationweit oberhalb von NavDisplay.
 * - CarScreen bleibt für Eingabefelder, Bildvorschau und Benutzeraktionen
 *   zuständig und erhält diese Abhängigkeiten nur über Parameter und Callbacks.
 */
