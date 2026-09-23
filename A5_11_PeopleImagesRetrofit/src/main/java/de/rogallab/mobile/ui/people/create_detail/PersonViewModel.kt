package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.domain.usecases.isLocalImagePath
import de.rogallab.mobile.shared.data.network.userMessageOr
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import de.rogallab.mobile.shared.R as SharedR

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _imageFileStorage: IImageFileStorage,
   private val _personUcCreate: PersonUcCreate,
   private val _personUcUpdate: PersonUcUpdate,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // A null or blank id means that a new person is being created.
   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null

   // Prevent duplicate repository writes while a Save operation is running.
   // This is internal processing state and therefore not part of PersonUiState.
   private var _isSaving = false

   // The initial state depends on whether a person is created or edited.
   private val _initialState =
      if (_isNew) PersonUiState(isNew = true, isLoading = false)
      else PersonUiState(isNew = false, isLoading = true)

   // Mutable PersonUiState is kept private inside the ViewModel.
   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)
   // Exposes the PersonUiState as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   // Load the existing person when the ViewModel is detail mode.
   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   // Loads an existing person from the repository
   private fun loadPerson(id: String) {
      viewModelScope.launch {
         // Indicate that the loading operation is in progress.
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         // Simulate a longer loading operation.
         delay(1000)

         _repository.findById(id)
            .onSuccess { person ->
               // A successful repository call may still return no matching person.
               if (person == null) {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  _effectDelegate.emit(PersonEffect.ShowError(error))

                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               // Store the loaded person and finish the loading operation.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person)
               }
            }
            .onFailure { throwable ->
               // Repository failures are converted into a localized UI effect.
               val fallback = _stringProvider.getString(R.string.error_person_load)
               val error = throwable.userMessageOr(fallback)
               _effectDelegate.emit(PersonEffect.ShowError(error))
            }

         // loading operation is finished, regardless of success or failure.
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = false)
         }

      }
   }

   // Dispatcher: Single public entry point for all events coming from the UI layer.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange ->changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)

         // Gallery selection must be copied from media store to app storage.
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)
         // Camera images already arrive as confirmed internal file paths.
         is PersonIntent.CameraImageTaken -> storeCameraImage(intent.imagePath)
         // The same intent is also used when an image is removed with null.
         is PersonIntent.RemoveImage -> removeImage(intent.imagePath)
         // Technical image errors are converted into the common error effect.
         is PersonIntent.ImageFailed -> showError(intent.message)

         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   // Update only the first name while keeping all other state values.
   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }

   // Update only the last name while keeping all other state values.
   private fun changeLastName(lastName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }

   // Updates the optional email address while keeping all other state values.
   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty())  emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   // Updates the optional phone number while keeping all other state values.
   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // A gallery image initially belongs to the external Photo Picker.
   // Copy it into private app storage before using it in the edit session.
   private fun storeGalleryImage(sourceUri: Uri) {
      viewModelScope.launch {
         // IImageFileStorage performs the technical Uri-to-file operation.
         val imagePath = _imageFileStorage
            .copyImageToAppStorage(sourceUri)
            .getOrElse {
               // The gallery image could not be stored, therefore the current
               // edit-session image remains unchanged.
               showError(_stringProvider.getString(SharedR.string.error_image_save))
               return@launch
            }
         // From this point on the feature works only with an internal path.
         replaceImage(imagePath)
      }
   }

   // Camera images already arrive as internal paths.
   private fun storeCameraImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   // Passing null removes the image from the current edit-session selection.
   private fun removeImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }


   // Replace the selected image reference. Only local files belong to Android.
   // A persisted HTTP(S) URL belongs to PeopleImagesApi and is never deleted here.
   private suspend fun replaceImage(imagePath: String?) {
      val previousImagePath = _stateFlow.value.person.imagePath

      if (previousImagePath.isLocalImagePath() && previousImagePath != imagePath)
         deleteTemporaryImageQuietly(previousImagePath)

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(imagePath = imagePath))
      }
   }

   // Send an error as a one-time effect instead of storing it in PersonUiState.
   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(PersonEffect.ShowError(message))
      }
   }

   // Validate and persist the current person.
   private fun save() {

      // Prevent multiple concurrent save operations.
      if (_isSaving) return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // Sanitize the email before validation.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            // Show the sanitized value in the UI as well.
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Sanitize the phone number before validation.
      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            // Show the sanitized value in the UI as well.
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Validate the complete entity before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         showError(error)
         return
      }

      // Publish the normalized and validated person before saving it.
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = person)
      }

      // Update: save operation is in progress.
      _isSaving = true

      viewModelScope.launch {

         // Delegate JSON, image transport and successful cleanup to a use case.
         val result =
            if (_isNew) _personUcCreate(person)
            else _personUcUpdate(person)

         result
            .onSuccess {
               // First show the success message...
               val message = _stringProvider.getString(R.string.message_person_saved, person.fullName,)
               _effectDelegate.emit(PersonEffect.ShowMessage(message))

               // ...and then request reverse navigation with Save semantics.
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure { throwable ->
               // Keep a local replacement after failure so Save can be retried.
               val fallback =
                  _stringProvider.getString(R.string.error_person_save)
               val error = throwable.userMessageOr(fallback)
               _effectDelegate.emit(PersonEffect.ShowError(error))
            }

         // Update: save operation is finished
         _isSaving = false
      }
   }

   // Discard the current edit session and navigate back without saving.
   private fun cancel() {
      viewModelScope.launch {

         // Remove only a local unsaved file. Remote URLs remain untouched.
         deleteTemporaryImageQuietly(_stateFlow.value.person.imagePath)

         // Navigation is emitted only after image cleanup has completed.
         _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
      }
   }

   private suspend fun deleteTemporaryImageQuietly(imagePath: String?) {
      if (!imagePath.isLocalImagePath()) return

      _imageFileStorage
         .deleteImageFromAppStorage(imagePath)
         .onFailure { throwable ->
            Alog.e(TAG, "delete temporary image failed: ${throwable.message}")
         }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonViewModel bleibt der zentrale Zustands- und Intent-Verarbeiter für
 *   die Personenbearbeitung. Der Screen beobachtet weiterhin genau einen
 *   StateFlow<PersonUiState>, während einmalige Meldungen und Navigationen
 *   getrennt über PersonEffect ausgegeben werden.
 * - Klassifizierte Netzwerkfehler werden als fertige Shared-Meldung angezeigt.
 *   Das ViewModel kennt weiterhin keine Retrofit-, OkHttp- oder HTTP-Typen.
 * - PersonUcCreate und PersonUcUpdate koordinieren Repository und das Aufräumen
 *   lokaler Transportdateien nach einem erfolgreichen Serverzugriff.
 *
 * - Alle UI-Ereignisse werden über die öffentliche Methode onIntent(...)
 *   verarbeitet. Die eigentliche Logik bleibt in privaten ViewModel-Funktionen.
 *   Dadurch besitzt das ViewModel weiterhin genau einen klaren Einstiegspunkt
 *   für Events aus der UI.
 *
 * - Die Bildbearbeitung ergänzt den bisherigen Personen-Workflow, ohne einen
 *   zweiten UI-State einzuführen. Das aktuelle Bild bleibt Bestandteil von
 *   PersonUiState und damit des bestehenden unidirektionalen Datenflusses.
 *
 * - Galerie und Kamera liefern dem ViewModel unterschiedliche Ausgangsdaten:
 *
 *      Galerie
 *          -> Content-Uri
 *
 *      Kamera
 *          -> bereits bestätigter interner Dateipfad
 *
 * - Eine Content-Uri aus dem Android Photo Picker darf nicht direkt als
 *   dauerhafte Bildreferenz der Person verwendet werden. Deshalb kopiert
 *   PersonViewModel ein Galerie-Bild zunächst über IImageFileStorage in den
 *   privaten App-Speicher:
 *
 *      GalleryPickerHandler
 *          -> Uri
 *          -> PersonIntent.GalleryImageSelected
 *          -> PersonViewModel
 *          -> IImageFileStorage.copyImageToAppStorage(...)
 *          -> interner imagePath
 *
 * - Ein Kamera-Bild wurde dagegen bereits durch CameraPickerHandler vorbereitet
 *   und nach erfolgreicher Aufnahme bestätigt. PersonViewModel erhält deshalb
 *   direkt den internen Dateipfad über PersonIntent.CameraImageTaken.
 *
 * - Nach diesem technischen Unterschied werden Galerie- und Kamera-Bilder gleich
 *   behandelt. In beiden Fällen enthält Person.imagePath zunächst einen lokalen
 *   Pfad, der beim Speichern als Datei hochgeladen wird.
 *
 * - Persistierte Bilder unterscheiden sich grundlegend von A5_10: Nach dem
 *   Upload enthält Person.imagePath eine HTTP(S)-URL der PeopleImagesApi. Diese
 *   URL gehört nicht dem lokalen Dateispeicher und darf dort nie gelöscht werden.
 *
 * - replaceImage(...) löscht deshalb nur eine zuvor gewählte lokale Ersatzdatei.
 *   Eine Server-URL bleibt bestehen, bis das Repository den getrennten Upload-
 *   oder Delete-Endpunkt erfolgreich aufgerufen hat.
 *
 * - Beim erfolgreichen Speichern gilt folgende Reihenfolge:
 *
 *      Person validieren
 *          -> Repository.create/update(...)
 *          -> erfolgreich
 *          -> temporäre lokale Datei löschen
 *          -> ShowMessage
 *          -> NavigateBack
 *
 * - Die lokale Datei wird bewusst erst nach erfolgreichem Repository-Zugriff
 *   gelöscht. Erst dann besitzt PeopleImagesApi eine eigene persistente Kopie.
 *   Bei einem Fehler bleibt die lokale Datei für einen erneuten Versuch erhalten.
 *
 * - Beim Abbrechen gilt:
 *
 *      Cancel
 *          -> temporäre lokale Datei löschen
 *          -> Server-URL unverändert lassen
 *          -> NavigateBack
 *
 * - Die Navigation wird auch hier erst nach dem Aufräumen der Edit-Session
 *   ausgelöst. Dadurch bleibt die Bildverwaltung vollständig abgeschlossen,
 *   bevor der PersonScreen verlassen wird.
 *
 * - _isSaving ist bewusst kein Bestandteil von PersonUiState. Die Variable
 *   verhindert lediglich parallele Repository-Schreibvorgänge und wird von
 *   der UI nicht dargestellt. Technischer interner Zustand muss daher nicht
 *   automatisch Teil des beobachtbaren UI-States sein.
 *
 * - Fehlermeldungen werden ebenfalls nicht dauerhaft im PersonUiState
 *   gespeichert. Repository-, Validierungs- und Bildfehler werden als
 *   PersonEffect.ShowError ausgegeben und von der UI einmalig verarbeitet.
 *
 * - Damit bleibt der bekannte Datenfluss erhalten:
 *
 *      PersonScreen
 *          -> Callback
 *
 *      PersonAdapter
 *          -> PersonIntent
 *
 *      PersonViewModel
 *          -> StateFlow<PersonUiState>
 *          -> PersonEffect
 *
 *      PersonAdapter
 *          -> PersonScreen / Snackbar / Navigation
 *
 * Lernziele:
 *
 * - Einen einzigen Intent-Einstiegspunkt im ViewModel verwenden.
 * - Persistent State und einmalige Effects voneinander unterscheiden.
 * - Content-Uri und internen Dateipfad unterscheiden.
 * - Galerie-Bilder vor der weiteren Verarbeitung in den App-Speicher kopieren.
 * - Technische Dateiverwaltung über IImageFileStorage kapseln.
 * - Lokale Bilddateien und entfernte Server-URLs sicher unterscheiden.
 * - Temporäre Dateien erst nach erfolgreichem Upload löschen.
 * - Cancel zum Aufräumen einer nicht gespeicherten lokalen Datei verwenden.
 * - Bestehenden UDF-/MVI-Datenfluss auch bei komplexerer Bildlogik beibehalten.
 */
