package de.rogallab.mobile.shared.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import kotlinx.coroutines.launch

/**
 * Determines whether the gallery launcher selects one image or multiple images.
 */
enum class ImageSelectionMode {
   Single,
   Multiple,
}

/**
 * Actions exposed by ImagePickerHandler to project-specific buttons or menus.
 */
@Stable
data class ImagePickerActions(
   val isBusy: Boolean,
   val selectFromGallery: () -> Unit,
   val takePhoto: () -> Unit,
)

/**
 * Connects Compose Activity Result launchers with IImageFileStorage.
 *
 * Selected images are copied immediately into private app storage. Camera files
 * are added only after TakePicture reports success. A cancelled camera operation
 * deletes its empty provisional file, preventing blank images in the UI state.
 *
 * The handler does not export images to MediaStore. Export is a separate user
 * action implemented through IImageMediaStore.
 */
@Composable
fun ImagePickerHandler(
   imageFileStorage: IImageFileStorage,
   selectionMode: ImageSelectionMode = ImageSelectionMode.Single,
   maxSelectionCount: Int = 10,
   onImagesStored: (List<String>) -> Unit,
   onError: (Throwable) -> Unit,
   content: @Composable (ImagePickerActions) -> Unit,
) {
   val coroutineScope = rememberCoroutineScope()
   val currentImageFileStorage by rememberUpdatedState(imageFileStorage)
   val currentOnImagesStored by rememberUpdatedState(onImagesStored)
   val currentOnError by rememberUpdatedState(onError)

   var isBusy by rememberSaveable { mutableStateOf(false) }
   var pendingCameraImagePath by rememberSaveable {
      mutableStateOf<String?>(null)
   }

   // Stores the selected images in private app storage and calls onImagesStored.
   fun storeSelectedUris(
      sourceUris: List<Uri>,
   ) {
      if (sourceUris.isEmpty())
         return

      isBusy = true
      coroutineScope.launch {
         val storedPaths = mutableListOf<String>()

         for (sourceUri in sourceUris) {
            // Copy the selected image to private app storage. If the copy fails, delete
            // any images that were already stored and call onError.
            val imagePath = currentImageFileStorage
               .copyImageToAppStorage(sourceUri)
               .getOrElse { throwable ->
                  storedPaths.forEach { storedPath ->
                     currentImageFileStorage
                        .deleteImageFromAppStorage(storedPath)
                  }
                  isBusy = false
                  currentOnError(throwable)
                  return@launch
               }

            storedPaths += imagePath
         }

         isBusy = false
         currentOnImagesStored(storedPaths)
      }
   }

   // ActivityResultLaunchers for gallery and camera actions. The launchers are
   // remembered to avoid re-creating them on every recomposition.
   val singleImageLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
   ) { selectedUri ->
      selectedUri?.let { sourceUri ->
         storeSelectedUris(listOf(sourceUri))
      }
   }

   // The PickMultipleVisualMedia contract is only available on Android 14+.
   val multipleImageLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(
         maxItems = maxSelectionCount.coerceAtLeast(2),
      ),
   ) { selectedUris ->
      storeSelectedUris(selectedUris)
   }

   // The TakePicture contract requires a provisional file to be created before
   val cameraLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture(),
   ) { success ->
      val imagePath = pendingCameraImagePath
      pendingCameraImagePath = null

      coroutineScope.launch {
         // If TakePicture was successful, confirm the provisional file and call onImagesStored.
         // Otherwise, delete the provisional file and call onError.
         if (success && imagePath != null) {
            currentImageFileStorage
               .confirmCameraImageFile(imagePath)
               .onSuccess { confirmedImagePath ->
                  currentOnImagesStored(listOf(confirmedImagePath))
               }
               .onFailure { throwable ->
                  currentImageFileStorage.deleteImageFromAppStorage(imagePath)
                  currentOnError(throwable)
               }
         }
         else {
            currentImageFileStorage
               .deleteImageFromAppStorage(imagePath)
               .onFailure { throwable ->
                  currentOnError(throwable)
               }
         }
         isBusy = false
      }
   }

   // The PickVisualMediaRequest is remembered to avoid re-creating it on every recomposition.
   val imageRequest = remember {
      PickVisualMediaRequest(
         ActivityResultContracts.PickVisualMedia.ImageOnly,
      )
   }

   // The ImagePickerActions are remembered to avoid re-creating them on every recomposition.
   val actions = ImagePickerActions(
      isBusy = isBusy,
      selectFromGallery = {
         if (!isBusy) {
            when (selectionMode) {
               ImageSelectionMode.Single ->
                  singleImageLauncher.launch(imageRequest)

               ImageSelectionMode.Multiple ->
                  multipleImageLauncher.launch(imageRequest)
            }
         }
      },
      takePhoto = {
         if (!isBusy) {
            isBusy = true
            coroutineScope.launch {
               val cameraImageFile = currentImageFileStorage
                  .createCameraImageFile()
                  .getOrElse { throwable ->
                     isBusy = false
                     currentOnError(throwable)
                     return@launch
                  }

               pendingCameraImagePath = cameraImageFile.imagePath

               try {
                  cameraLauncher.launch(cameraImageFile.contentUri)
               }
               catch (throwable: Throwable) {
                  pendingCameraImagePath = null
                  currentImageFileStorage
                     .deleteImageFromAppStorage(cameraImageFile.imagePath)
                  isBusy = false
                  currentOnError(throwable)
               }
            }
         }
      },
   )

   content(actions)
}

/*
 * Didaktik und Lernziele
 *
 * - ImagePickerHandler kapselt die Activity-Result-APIs für Galerie und Kamera.
 *   Die aufrufende UI erhält nur einfache Funktionen in ImagePickerActions.
 *
 * - Galerie-Bilder werden nach der Auswahl in den privaten App-Speicher
 *   kopiert. Dadurch ist die Anwendung nicht dauerhaft von einer externen
 *   Content-URI abhängig.
 *
 * - Für die Kamera wird zunächst eine vorläufige Datei erzeugt. Nur wenn
 *   TakePicture erfolgreich war und die Datei Inhalt besitzt, wird ihr Pfad
 *   an die UI weitergegeben. Bei Abbruch wird die vorläufige Datei gelöscht.
 *
 * - isBusy verhindert parallele Picker-/Kamera-Aktionen während einer laufenden
 *   Speicheroperation.
 *
 * Lernziele:
 *
 * - ActivityResultContracts in Compose einsetzen.
 * - Photo Picker und Kamera hinter einer gemeinsamen Abstraktion kapseln.
 * - Temporäre Kamera-Dateien sicher behandeln.
 * - Suspendierende Dateioperationen in einem Compose-gebundenen Scope ausführen.
 */
