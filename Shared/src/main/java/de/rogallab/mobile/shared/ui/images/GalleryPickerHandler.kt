package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Determines whether one or multiple images can be selected from the gallery.
 */
enum class GallerySelectionMode {
   Single,
   Multiple,
}

/**
 * Actions exposed by GalleryPickerHandler to the calling adapter.
 */
@Stable
data class GalleryPickerActions(
   val selectFromGallery: () -> Unit,
)

/**
 * Encapsulates the Android Photo Picker.
 *
 * The handler only selects images and returns their content URIs. Copying the
 * selected images into private app storage is deliberately not part of this
 * UI component and is handled by the ViewModel through IImageFileStorage.
 */
@Composable
fun GalleryPickerHandler(
   selectionMode: GallerySelectionMode = GallerySelectionMode.Single,
   maxSelectionCount: Int = 10,
   onImagesSelected: (List<Uri>) -> Unit,
   content: @Composable (GalleryPickerActions) -> Unit,
) {

   // Keep the latest callback without recreating the Activity Result launchers
   // whenever the surrounding composable is recomposed.
   val currentOnImagesSelected =
      rememberUpdatedState(onImagesSelected)

   // Create one reusable request that accepts image media only.
   // The request itself contains no launcher and does not open the picker yet.
   val imageRequest = remember {
      PickVisualMediaRequest(
         ActivityResultContracts.PickVisualMedia.ImageOnly,
      )
   }

   // Register the launcher for selecting exactly one image.
   // The launcher is remembered by Compose and survives recompositions.
   val singleImageLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia(),
   ) { selectedUri ->

      // A null URI means that the user closed the picker without selecting
      // an image. In that case no application callback is emitted.
      selectedUri?.let { uri ->

         // Normalize the result to List<Uri> so that Single and Multiple
         // selection can use the same callback interface.
         currentOnImagesSelected.value(
            listOf(uri)
         )
      }
   }

   // Register a separate launcher for selecting multiple images.
   // Android requires a maximum number of selectable items.
   val multipleImageLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickMultipleVisualMedia(
         // Multiple selection requires at least two selectable items.
         maxItems = maxSelectionCount.coerceAtLeast(2),
      ),
   ) { selectedUris ->

      // An empty list means that no image was selected.
      if (selectedUris.isNotEmpty()) {

         // Forward the selected content URIs unchanged.
         // Persisting these images is the responsibility of the caller.
         currentOnImagesSelected.value(
            selectedUris
         )
      }
   }

   // Expose a small UI-facing action object instead of exposing the launchers
   // themselves. The caller therefore does not need to know which Android
   // Activity Result contract is used internally.
   val actions = GalleryPickerActions(

      // Open the correct Photo Picker depending on the configured mode.
      selectFromGallery = {
         when (selectionMode) {

            // Start the single-selection Photo Picker.
            GallerySelectionMode.Single ->
               singleImageLauncher.launch(imageRequest)

            // Start the multiple-selection Photo Picker.
            GallerySelectionMode.Multiple ->
               multipleImageLauncher.launch(imageRequest)
         }
      },
   )

   // Pass the available gallery actions to the surrounding UI.
   // The content composable decides where and how the action is triggered.
   content(actions)
}

/*
 * Didaktik und Lernziele
 *
 * - GalleryPickerHandler kapselt ausschließlich den Android Photo Picker.
 *   Er übernimmt damit nur die technische Auswahl bereits vorhandener Bilder.
 *
 * - Der Handler stellt dem aufrufenden Adapter keine ActivityResultLauncher
 *   direkt zur Verfügung. Stattdessen erhält die UI mit GalleryPickerActions
 *   eine kleine, stabile Schnittstelle:
 *
 *      selectFromGallery()
 *
 * - Erst beim Aufruf dieser Funktion wird der Android Photo Picker geöffnet.
 *   Abhängig von GallerySelectionMode wird entweder der Launcher für ein
 *   einzelnes Bild oder der Launcher für mehrere Bilder verwendet.
 *
 * - Für Single und Multiple werden bewusst zwei verschiedene
 *   ActivityResultContracts eingesetzt:
 *
 *      PickVisualMedia
 *          Auswahl genau eines Bildes
 *
 *      PickMultipleVisualMedia
 *          Auswahl mehrerer Bilder
 *
 * - Beide Varianten liefern am Ende dieselbe Form von Ergebnis:
 *
 *      List<Uri>
 *
 *   Bei einer Einzelauswahl wird die einzelne Uri deshalb in eine Liste mit
 *   genau einem Element umgewandelt. Dadurch benötigt der aufrufende Code nur
 *   eine gemeinsame Callback-Schnittstelle.
 *
 * - Die zurückgegebenen Uri-Objekte sind Content-URIs des Android Photo Pickers.
 *   Sie sind noch keine internen Dateipfade der Anwendung.
 *
 * - GalleryPickerHandler kopiert die ausgewählten Bilder deshalb bewusst nicht
 *   selbst in den privaten App-Speicher. Diese Aufgabe wird später durch
 *   IImageFileStorage übernommen.
 *
 * - Damit bleiben zwei Verantwortungen getrennt:
 *
 *      GalleryPickerHandler
 *          Bild auswählen
 *          -> Uri liefern
 *
 *      IImageFileStorage
 *          Bilddatei technisch in den App-Speicher kopieren
 *          -> internen Dateipfad liefern
 *
 * - rememberLauncherForActivityResult bindet die Activity Result API an den
 *   Compose-Lebenszyklus. Die registrierten Launcher müssen deshalb nicht bei
 *   jeder Recomposition neu erzeugt werden.
 *
 * - rememberUpdatedState wird für onImagesSelected verwendet, damit die
 *   Launcher auch nach Recompositions immer den aktuellsten Callback aufrufen,
 *   ohne deshalb neu registriert werden zu müssen.
 *
 * - PickVisualMediaRequest beschreibt nur, welche Medien ausgewählt werden
 *   dürfen. Mit ImageOnly wird der Photo Picker auf Bilder eingeschränkt.
 *
 * - Ein Abbruch der Bildauswahl erzeugt kein fachliches Ereignis:
 *
 *      Single   -> selectedUri == null
 *      Multiple -> selectedUris.isEmpty()
 *
 *   In beiden Fällen wird onImagesSelected nicht aufgerufen.
 *
 * - GalleryPickerHandler enthält keinen fachlichen Zustand und keinen
 *   StateFlow. Er ist lediglich eine wiederverwendbare technische Brücke
 *   zwischen Jetpack Compose und der Android Activity Result API.
 *
 * Lernziele:
 *
 * - ActivityResultContracts in Jetpack Compose verwenden.
 * - rememberLauncherForActivityResult an den Compose-Lebenszyklus binden.
 * - rememberUpdatedState für aktuelle Callback-Referenzen einsetzen.
 * - Single- und Multiple-Selection mit unterschiedlichen Contracts umsetzen.
 * - Unterschiedliche Android-Ergebnisse auf eine gemeinsame Schnittstelle
 *   List<Uri> vereinheitlichen.
 * - Content-Uri und internen Dateipfad voneinander unterscheiden.
 * - Bildauswahl und Dateispeicherung als getrennte Verantwortungen verstehen.
 */