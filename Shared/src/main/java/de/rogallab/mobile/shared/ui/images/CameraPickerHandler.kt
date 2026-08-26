package de.rogallab.mobile.shared.ui.images

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import kotlinx.coroutines.launch

/**
 * Actions exposed by CameraPickerHandler to the calling adapter.
 */
@Stable
data class CameraPickerActions(
   val isBusy: Boolean,
   val takePhoto: () -> Unit,
)

/**
 * Encapsulates the TakePicture Activity Result contract.
 *
 * A camera application needs a destination URI before it is started. Therefore
 * this handler creates a provisional private file through IImageFileStorage,
 * launches the camera with its FileProvider URI and confirms the file after a
 * successful capture. Cancelled or failed captures remove the provisional file.
 */
@Composable
fun CameraPickerHandler(
   imageFileStorage: IImageFileStorage,
   onPhotoStored: (String) -> Unit,
   onError: (Throwable) -> Unit,
   content: @Composable (CameraPickerActions) -> Unit,
) {

   // Create a coroutine scope bound to the lifecycle of this composable.
   val coroutineScope = rememberCoroutineScope()

   // Keep the latest references without recreating the Activity Result launcher
   // whenever the surrounding composable is recomposed.
   val currentImageFileStorage by rememberUpdatedState(imageFileStorage)
   val currentOnPhotoStored by rememberUpdatedState(onPhotoStored)
   val currentOnError by rememberUpdatedState(onError)

   // Prevent overlapping camera operations while a file is being prepared,
   // the camera is active or the result is currently being processed.
   var isBusy by rememberSaveable { mutableStateOf(false) }

   // Remember the path of the provisional camera file while the external
   // camera application is active.
   // The Activity Result callback only returns a Boolean success value.
   // Therefore the associated file path must be stored separately.
   var pendingCameraImagePath by rememberSaveable { mutableStateOf<String?>(null) }

   // Register the Android TakePicture contract.
   // TakePicture receives a destination Uri when launched and returns only
   // whether the camera application successfully wrote an image to that Uri.
   val cameraLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.TakePicture(),
   ) { success ->
      // Take a local copy of the pending path before clearing the Compose state.
      val imagePath = pendingCameraImagePath
      // The camera operation has finished, so no file remains pending.
      pendingCameraImagePath = null

      coroutineScope.launch {
         if (success && imagePath != null) {
            // A successful TakePicture result means that the camera application
            // claims to have written the file. Let the storage layer verify and
            // finalize the provisional image before exposing it to the feature.
            currentImageFileStorage
               .confirmCameraImageFile(imagePath)
               .onSuccess { confirmedImagePath ->
                  // Only confirmed internal file paths are forwarded to
                  // PersonAdapter and eventually to the ImageEditDelegate.
                  currentOnPhotoStored(confirmedImagePath)
               }
               .onFailure { throwable ->
                  // Confirmation failed. The provisional image must not remain
                  // in app storage because it is not a valid edit-session image.
                  currentImageFileStorage.deleteImageFromAppStorage(imagePath)

                  currentOnError(throwable)
               }
         }
         else {
            // The user cancelled the camera or the capture failed.
            // In both cases the provisional target file is no longer needed.
            currentImageFileStorage
               .deleteImageFromAppStorage(imagePath)
               .onFailure { throwable ->
                  // Deletion errors are reported as technical image errors.
                  currentOnError(throwable)
               }
         }

         // The complete camera operation, including cleanup or confirmation,
         // has finished and a new camera request may now be started.
         isBusy = false
      }
   }

   // Expose a small UI-facing action object instead of exposing the launcher
   // and storage operations directly to PersonAdapter.
   val actions = CameraPickerActions(
      isBusy = isBusy,

      takePhoto = {
         // Ignore additional requests while another camera operation is active.
         if (!isBusy) {
            isBusy = true

            coroutineScope.launch {
               // Create the provisional target file before opening the camera.
               //
               // This is the key difference from the gallery flow:
               // the camera needs a writable destination Uri in advance.
               val cameraImageFile = currentImageFileStorage
                  .createCameraImageFile()
                  .getOrElse { throwable ->
                     // File creation failed, therefore the camera cannot start.
                     isBusy = false
                     currentOnError(throwable)
                     return@launch
                  }

               // Store the internal path so that it is available again when
               // the asynchronous Activity Result callback is invoked.
               pendingCameraImagePath = cameraImageFile.imagePath

               try {
                  // Start external camera with the contentUri as destination.
                  cameraLauncher.launch(cameraImageFile.contentUri)
               }
               catch (throwable: Throwable) {
                  // Launching the camera itself failed. Remove provisional file.
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

   // Pass the camera actions to the surrounding UI.
   // The content composable decides where the camera action is triggered.
   content(actions)
}

/*
 * Didaktik und Lernziele
 *
 * - CameraPickerHandler kapselt ausschließlich den technischen Ablauf einer
 *   Kameraaufnahme mit der Android Activity Result API.
 *
 * - Der Kamera-Ablauf unterscheidet sich grundlegend von der Galerieauswahl.
 *   Bei der Galerie wird zuerst ein vorhandenes Bild ausgewählt und anschließend
 *   in den App-Speicher kopiert. Eine Kamera benötigt dagegen bereits vor ihrem
 *   Start eine Datei beziehungsweise eine Uri, in die sie das neue Bild
 *   schreiben kann.
 *
 * - Der Ablauf ist deshalb:
 *
 *      takePhoto()
 *          -> IImageFileStorage.createCameraImageFile()
 *          -> CameraImageFile
 *             - imagePath
 *             - contentUri
 *          -> ActivityResultContracts.TakePicture
 *          -> Kamera-Anwendung
 *
 * - CameraImageFile enthält zwei unterschiedliche Referenzen auf dasselbe
 *   vorbereitete Bild:
 *
 *      contentUri
 *          wird an die externe Kamera-Anwendung übergeben
 *
 *      imagePath
 *          bezeichnet die Datei innerhalb des privaten App-Speichers
 *
 * - Die externe Kamera erhält nur die FileProvider-Uri. Ein direkter Dateipfad
 *   wird nicht an eine andere Anwendung weitergegeben.
 *
 * - ActivityResultContracts.TakePicture liefert nach Beendigung der Kamera
 *   kein Bild und keine Uri zurück. Das Ergebnis ist lediglich ein Boolean:
 *
 *      true
 *          die Kamera meldet eine erfolgreiche Aufnahme
 *
 *      false
 *          Aufnahme wurde abgebrochen oder ist fehlgeschlagen
 *
 * - Deshalb muss pendingCameraImagePath während des Kameraaufrufs separat
 *   gespeichert werden. Erst damit kann der Activity-Result-Callback das
 *   Ergebnis wieder der zuvor vorbereiteten Datei zuordnen.
 *
 * - rememberSaveable wird für isBusy und pendingCameraImagePath verwendet.
 *   Dadurch bleiben diese kleinen UI-bezogenen Zustände auch bei einer
 *   Recreation der Compose-Struktur erhalten, soweit Android sie speichern
 *   kann.
 *
 * - rememberUpdatedState hält jeweils die aktuellste Referenz auf
 *   imageFileStorage, onPhotoStored und onError bereit. Der registrierte
 *   ActivityResultLauncher muss dadurch bei einer Recomposition nicht neu
 *   erzeugt werden und verwendet trotzdem die aktuellen Callback-Funktionen.
 *
 * - Nach einer erfolgreichen Aufnahme wird die vorbereitete Datei nicht sofort
 *   weitergegeben. Zunächst ruft CameraPickerHandler
 *
 *      IImageFileStorage.confirmCameraImageFile(...)
 *
 *   auf. Erst wenn die Datei bestätigt wurde, erhält der aufrufende Adapter
 *   über onPhotoStored den internen Dateipfad.
 *
 * - Damit lautet der erfolgreiche Kamera-Ablauf:
 *
 *      CameraPickerHandler
 *          -> createCameraImageFile()
 *          -> contentUri
 *          -> Kamera
 *          -> TakePicture result = true
 *          -> confirmCameraImageFile(...)
 *          -> imagePath
 *          -> onPhotoStored(imagePath)
 *
 * - Wird die Aufnahme abgebrochen, schlägt sie fehl oder kann die Datei nicht
 *   bestätigt werden, wird die zuvor vorbereitete Datei wieder aus dem
 *   privaten App-Speicher gelöscht.
 *
 * - Dadurch bleiben keine leeren oder unvollständigen Kamera-Dateien zurück:
 *
 *      Abbruch
 *          -> deleteImageFromAppStorage(...)
 *
 *      Fehler beim Kamera-Start
 *          -> deleteImageFromAppStorage(...)
 *
 *      Fehler bei confirmCameraImageFile(...)
 *          -> deleteImageFromAppStorage(...)
 *
 * - isBusy verhindert, dass während einer laufenden Kameraoperation eine zweite
 *   Aufnahme gestartet wird. Der Zustand umfasst dabei nicht nur die sichtbare
 *   Kamera-Anwendung, sondern auch das Erzeugen, Bestätigen und gegebenenfalls
 *   Löschen der zugehörigen Datei.
 *
 * - CameraPickerHandler übernimmt nur den technischen Lebenszyklus der
 *   Kamera-Datei bis zu ihrer erfolgreichen Bestätigung.
 *
 * - Die fachliche Lebensdauer des Bildes während der Personenbearbeitung ist
 *   dagegen nicht Aufgabe des CameraPickerHandler. Sobald onPhotoStored einen
 *   bestätigten internen Dateipfad liefert, übernimmt ImageEditDelegate die
 *   weitere Verwaltung innerhalb der Edit-Session.
 *
 * - Damit sind die Verantwortlichkeiten klar getrennt:
 *
 *      CameraPickerHandler
 *          Kamera starten
 *          temporäre Zieldatei vorbereiten
 *          Kameraergebnis auswerten
 *          Datei bestätigen oder aufräumen
 *
 *      IImageFileStorage
 *          technische Dateioperationen durchführen
 *
 *      ImageEditDelegate
 *          bestätigte Bilder innerhalb einer Edit-Session verwalten
 *
 * - Der aufrufende PersonAdapter kennt die technischen Details des
 *   TakePicture-Contracts nicht. Er erhält lediglich CameraPickerActions:
 *
 *      isBusy
 *      takePhoto()
 *
 *   sowie über onPhotoStored den fertigen internen Dateipfad.
 *
 * Lernziele:
 *
 * - ActivityResultContracts.TakePicture in Jetpack Compose einsetzen.
 * - Verstehen, warum eine Kamera vor ihrem Start eine Ziel-Uri benötigt.
 * - FileProvider-Uri und internen Dateipfad unterscheiden.
 * - Asynchrone Activity-Result-Aufrufe mit zuvor erzeugten Dateien verknüpfen.
 * - rememberUpdatedState für langlebige Activity-Result-Callbacks einsetzen.
 * - rememberSaveable für kleinen technischen UI-Zustand verwenden.
 * - Temporäre Kamera-Dateien bei Abbruch und Fehlern zuverlässig aufräumen.
 * - Technischen Kamera-Dateilebenszyklus und fachliche Edit-Session trennen.
 */