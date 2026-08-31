package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun CarAdapter(
   viewModel: CarViewModel,
   contentPadding: PaddingValues,
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
   val remainingSlots =
      (MAX_CAR_IMAGE_COUNT - imageCount).coerceAtLeast(1)
   val canAddMoreImages = imageCount < MAX_CAR_IMAGE_COUNT
   val imageSaveError = stringResource(SharedR.string.error_image_save)

   GalleryPickerHandler(
      selectionMode =
         if (remainingSlots == 1) GallerySelectionMode.Single
         else GallerySelectionMode.Multiple,
      maxSelectionCount = remainingSlots,
      onImagesSelected = { sourceUris -> viewModel.onIntent(CarIntent.GalleryImagesSelected(sourceUris)) },
   ) { galleryActions ->

      CameraPickerHandler(
         imageFileStorage = imageFileStorage,
         onPhotoStored = { imagePath -> viewModel.onIntent(CarIntent.CameraImageTaken(imagePath)) },
         onError = { viewModel.onIntent(CarIntent.ImageFailed(imageSaveError)) },
      ) { cameraActions ->

         CarScreen(
            carUiState = carUiState,
            validator = validator,
            contentPadding = contentPadding,
            onSelectImages = { if (canAddMoreImages) galleryActions.selectFromGallery() },
            onTakePhoto = { if (canAddMoreImages) cameraActions.takePhoto() },
            onIntent = viewModel::onIntent,
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - CarAdapter bindet ausschließlich die bereits vorhandenen Shared-Picker an
 *   das Fahrzeug-Feature. Android ActivityResult-Details bleiben damit aus
 *   CarScreen und CarViewModel heraus.
 *
 * - GalleryPickerHandler liefert URIs, CameraPickerHandler einen bestätigten
 *   internen Dateipfad. Beide Ergebnisse werden als Intents an das ViewModel
 *   weitergereicht.
 */
