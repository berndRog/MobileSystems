
package de.rogallab.mobile.shared.ui.images

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Encapsulates the TakePicture Activity Result contract.
 *
 * A camera application needs a destination Uri before it is started.
 * The handler creates a provisional private file through IImageFileStorage,
 * launches the camera with its FileProvider Uri and confirms the file
 * after a successful capture.
 *
 * Cancelled or failed captures remove the provisional file.
 */
@Composable
fun CameraPickerHandler(
   imageFileStorage: IImageFileStorage,
   onPhotoStored: (String) -> Unit,
   onError: (Throwable) -> Unit,
   content: @Composable (CameraPickerActions) -> Unit,
) {

   // Create a coroutine scope bound to this composable.
   val coroutineScope = rememberCoroutineScope()

   // Keep the latest references for Activity Result callbacks.
   val currentImageFileStorage by rememberUpdatedState(imageFileStorage)
   val currentOnPhotoStored by rememberUpdatedState(onPhotoStored)
   val currentOnError by rememberUpdatedState(onError)

   // Short-lived states for preparation and result processing.
   // These coroutines do not survive leaving the composition.
   var isPreparing by remember { mutableStateOf(false) }
   var isProcessing by remember { mutableStateOf(false) }

   // Preserve the pending file path across Activity recreation.
   // The TakePicture callback only returns a Boolean.
   var pendingCameraImagePath by rememberSaveable {
      mutableStateOf<String?>(null)
   }

   // Derive the busy state from the individual operation states.
   val isBusy = isPreparing ||
      pendingCameraImagePath != null ||
      isProcessing

   // Register the TakePicture Activity Result contract.
   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture(),
   ) { success ->

      // Prevent a new operation while processing the result.
      isProcessing = true

      // Keep the path locally before clearing the saved state.
      val imagePath = pendingCameraImagePath
      pendingCameraImagePath = null

      coroutineScope.launch {
         try {
            if (success && imagePath != null) {

               // Verify and finalize the provisional image.
               currentImageFileStorage
                  // Verifies that the camera wrote a non-empty private image file.
                  .confirmCameraImageFile(imagePath)

                  .onSuccess { confirmedImagePath ->
                     // Forward only confirmed internal file paths.
                     currentOnPhotoStored(confirmedImagePath)
                  }

                  .onFailure { throwable ->
                     // Remove the provisional file if confirmation fails.
                     currentImageFileStorage
                        .deleteImageFromAppStorage(imagePath)
                        .onFailure(currentOnError)
                     currentOnError(throwable)
                  }
            }
            else {

               // Capture cancelled, failed or no pending path available.
               currentImageFileStorage
                  .deleteImageFromAppStorage(imagePath)
                  .onFailure(currentOnError)
            }
         }
         finally {
            // Result processing has finished.
            isProcessing = false
         }
      }
   }

   // Expose a small UI-facing action object.
   val actions = CameraPickerActions(
      isBusy = isBusy,

      takePhoto = {

         // Prevent overlapping camera operations.
         if (!isBusy) {
            isPreparing = true

            coroutineScope.launch {
               try {
                  // Create the target file before starting the camera.
                  val cameraImageFile = currentImageFileStorage
                     .createCameraImageFile()
                     .getOrElse { throwable ->
                        currentOnError(throwable)
                        return@launch
                     }

                  // Remember the path for the later result callback.
                  pendingCameraImagePath = cameraImageFile.imagePath

                  try {
                     // Start the external camera with its destination Uri.
                     launcher.launch(cameraImageFile.contentUri)
                  }
                  catch (throwable: Exception) {

                     // Camera launch failed. Remove the provisional file.
                     pendingCameraImagePath = null

                     currentImageFileStorage
                        .deleteImageFromAppStorage(cameraImageFile.imagePath)
                        .onFailure(currentOnError)

                     if (throwable is CancellationException) {
                        throw throwable
                     }
                     currentOnError(throwable)
                  }
               }
               finally {
                  // File preparation has finished.
                  isPreparing = false
               }
            }
         }
      },
   )

   // Pass the actions to the surrounding UI.
   content(actions)
}

/*
 * Didaktik und Lernziele
 *
 * - CameraPickerHandler kapselt den technischen Ablauf einer Kameraaufnahme
 *   mit der Android Activity Result API.
 *
 * - Anders als bei der Galerie muss die Zieldatei bereits vor dem Start
 *   der externen Kamera-Anwendung vorbereitet werden.
 *
 * - Der erfolgreiche Ablauf ist:
 *
 *      takePhoto()
 *          -> IImageFileStorage.createCameraImageFile()
 *          -> CameraImageFile
 *               imagePath
 *               contentUri
 *          -> cameraLauncher.launch(contentUri)
 *          -> Externe Kamera-Anwendung
 *          -> TakePicture result = true
 *          -> confirmCameraImageFile(imagePath)
 *          -> onPhotoStored(confirmedImagePath)
 *
 * - CameraImageFile enthält zwei Referenzen auf dasselbe Bild:
 *
 *      contentUri
 *          FileProvider-Uri für die externe Kamera-Anwendung.
 *
 *      imagePath
 *          Interner Dateipfad für die weitere Verarbeitung.
 *
 * - ActivityResultContracts.TakePicture liefert nur Boolean.
 *   Der Dateipfad muss deshalb separat gespeichert werden.
 *
 * - Die Zustandsverwaltung unterscheidet drei Phasen:
 *
 *      isPreparing
 *          Die vorläufige Zieldatei wird erzeugt.
 *
 *      pendingCameraImagePath != null
 *          Die Datei wurde vorbereitet und das Kameraergebnis
 *          steht noch aus.
 *
 *      isProcessing
 *          Das Kameraergebnis wird bestätigt oder aufgeräumt.
 *
 * - isBusy wird aus diesen Zuständen abgeleitet:
 *
 *      isBusy = isPreparing ||
 *               pendingCameraImagePath != null ||
 *               isProcessing
 *
 * - remember wird für kurzlebige Coroutine-Zustände verwendet.
 *
 * - rememberSaveable bewahrt den ausstehenden Dateipfad bei einer
 *   Activity-Recreation, soweit der Zustand wiederherstellbar ist.
 *
 * - rememberUpdatedState stellt den Callbacks die aktuellen
 *   Referenzen auf Storage und Callback-Funktionen bereit.
 *
 * - Bei erfolgreicher Aufnahme wird die Datei zunächst bestätigt.
 *   Erst danach wird der interne Dateipfad weitergegeben.
 *
 * - Bei Abbruch oder Fehler wird die vorläufige Datei gelöscht.
 *   Fehler beim Löschen werden über onError gemeldet.
 *
 * - try/finally sorgt dafür, dass die kurzlebigen Zustände
 *   nach der jeweiligen Operation zurückgesetzt werden.
 *
 * - CameraPickerHandler verwaltet nur die technische Lebensdauer
 *   der Kamera-Datei bis zu ihrer Bestätigung.
 *
 * - Danach übernimmt ImageEdit die Lebensdauer des Bildes
 *   innerhalb der Edit-Session.
 *
 * - PersonAdapter erhält lediglich CameraPickerActions:
 *
 *      isBusy
 *      takePhoto()
 *
 *   sowie über onPhotoStored den bestätigten Dateipfad.
 *
 * Lernziele:
 *
 * - ActivityResultContracts.TakePicture in Compose verwenden.
 * - FileProvider-Uri und internen Dateipfad unterscheiden.
 * - Asynchrone Activity-Ergebnisse einer Datei zuordnen.
 * - remember und rememberSaveable gezielt einsetzen.
 * - Zusammengesetzte UI-Zustände ableiten statt doppelt speichern.
 * - Vorläufige Dateien nach Abbruch oder Fehler aufräumen.
 * - Technische Dateioperationen von der Edit-Session trennen.
 */
