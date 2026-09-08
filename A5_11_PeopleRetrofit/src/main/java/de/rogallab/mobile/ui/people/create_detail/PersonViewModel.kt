package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
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
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // A null or blank id means that a new person is being created.
   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null

   // Prevent duplicate repository writes while a Save operation is running.
   private var _isSaving = false

   // The initial state depends on whether a person is created or edited.
   private val _initialState =
      if (_isNew) PersonUiState(isNew = true, isLoading = false)
      else PersonUiState(isNew = false, isLoading = true)

   // Mutable PersonUiState is kept private inside the ViewModel.
   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)

   // Expose PersonUiState as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   // Load an existing person from PeopleApi through the repository.
   private fun loadPerson(id: String) {
      viewModelScope.launch {
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         // Keep the visible teaching delay used by the previous examples.
         delay(1000)

         _repository.findById(id)
            .onSuccess { person ->
               if (person == null) {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  _effectDelegate.emit(PersonEffect.ShowError(error))

                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               // A persisted image is now an HTTP URL returned by PeopleApi.
               // It is displayed directly and is never owned by local file storage.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person, isLoading = false)
               }
            }
            .onFailure {
               val error = _stringProvider.getString(R.string.error_person_load)
               _effectDelegate.emit(PersonEffect.ShowError(error))

               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   // Single public entry point for all events coming from the UI layer.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)

         is PersonIntent.GalleryImageSelected ->
            storeGalleryImage(intent.sourceUri)
         is PersonIntent.CameraImageTaken ->
            storeCameraImage(intent.imagePath)
         is PersonIntent.RemoveImage ->
            removeImage(intent.imagePath)
         is PersonIntent.ImageFailed ->
            showError(intent.message)

         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }

   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // A gallery Uri is copied into private app storage before it can be uploaded.
   private fun storeGalleryImage(sourceUri: Uri) {
      viewModelScope.launch {
         val imagePath = _imageFileStorage
            .copyImageToAppStorage(sourceUri)
            .getOrElse {
               val error = _stringProvider.getString(R.string.error_image_save)
               showError(error)
               return@launch
            }

         replaceImage(imagePath)
      }
   }

   // Camera images already arrive as confirmed private local file paths.
   private fun storeCameraImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   // Passing null means that the server image should be removed on Save.
   private fun removeImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   // Replace the selected image reference. Only temporary local files belong to
   // this Android app. Persisted http(s) URLs belong to PeopleApi and must never
   // be passed to IImageFileStorage.deleteImageFromAppStorage(...).
   private suspend fun replaceImage(imagePath: String?) {
      val previousImagePath = _stateFlow.value.person.imagePath

      if (isLocalImagePath(previousImagePath) && previousImagePath != imagePath) {
         _imageFileStorage
            .deleteImageFromAppStorage(previousImagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "delete temporary replacement failed: ${throwable.message}")
            }
      }

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(imagePath = imagePath))
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(PersonEffect.ShowError(message))
      }
   }

   // Validate and persist the current person through PeopleApi.
   private fun save() {
      if (_isSaving) return

      var person = _stateFlow.value.person.normalized()

      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      val error = _validator.validatePerson(person)
      if (error != null) {
         showError(error)
         return
      }

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = person)
      }

      _isSaving = true

      viewModelScope.launch {
         val result =
            if (_isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               // The server has copied the multipart image into its own storage.
               // The local picker file is therefore obsolete after a successful
               // REST write and can now be deleted safely.
               deleteTemporaryImageQuietly(person.imagePath)

               val message = _stringProvider.getString(
                  R.string.message_person_saved,
                  person.fullName,
               )
               _effectDelegate.emit(PersonEffect.ShowMessage(message))
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure {
               // Keep a local replacement after a failed REST call so the user
               // can retry Save without selecting or taking the image again.
               val errorMessage =
                  _stringProvider.getString(R.string.error_person_save)
               _effectDelegate.emit(PersonEffect.ShowError(errorMessage))
            }

         _isSaving = false
      }
   }

   // Cancel discards only an unsaved local picker file. A persisted server URL is
   // just a remote reference and must remain untouched.
   private fun cancel() {
      viewModelScope.launch {
         deleteTemporaryImageQuietly(_stateFlow.value.person.imagePath)
         _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
      }
   }

   private suspend fun deleteTemporaryImageQuietly(imagePath: String?) {
      if (!isLocalImagePath(imagePath)) return

      _imageFileStorage
         .deleteImageFromAppStorage(imagePath)
         .onFailure { throwable ->
            Alog.e(TAG, "delete temporary image failed: ${throwable.message}")
         }
   }

   private fun isLocalImagePath(imagePath: String?): Boolean =
      !imagePath.isNullOrBlank() &&
         !imagePath.startsWith("http://") &&
         !imagePath.startsWith("https://")

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PersonViewModel behält den UDF-Ablauf aus A5_01: Der Screen sendet Intents,
 *   das ViewModel aktualisiert genau einen PersonUiState und erzeugt einmalige
 *   PersonEffects für Meldungen und Navigation.
 *
 * - Die wesentliche Änderung betrifft die Verantwortung für Bilder. In A5_01
 *   war das persistierte Bild eine lokale App-Datei. In A5_11 ist das persistierte
 *   Bild dagegen eine HTTP-URL, deren Datei vollständig von PeopleApi verwaltet
 *   wird. Eine Server-URL darf deshalb niemals lokal gelöscht werden.
 *
 * - Galerie und Kamera benötigen trotzdem zunächst eine lokale Datei:
 *
 *      Galerie / Kamera
 *          -> temporäre private Datei
 *          -> Person.imagePath
 *          -> Repository.create/update(...)
 *          -> multipart/form-data
 *          -> PeopleApi
 *          -> persistente ImageUrl
 *
 * - Der lokale Pfad ist nur Transportzustand bis zum Upload. Nach erfolgreichem
 *   POST oder PUT besitzt der Server eine eigene Kopie und die temporäre Android-
 *   Datei wird gelöscht.
 *
 * - Schlägt der REST-Aufruf fehl, bleibt die temporäre Datei bewusst erhalten.
 *   Der Benutzer kann den Speichervorgang dadurch erneut versuchen, ohne das Bild
 *   erneut auswählen oder aufnehmen zu müssen.
 *
 * - Beim Ersetzen eines noch nicht hochgeladenen lokalen Bildes kann die alte
 *   temporäre Datei sofort gelöscht werden. Beim Ersetzen oder Entfernen einer
 *   persistierten Server-URL erfolgt dagegen keine lokale Dateioperation.
 *
 * - Beim Entfernen eines persistierten Bildes setzt das ViewModel imagePath nur
 *   auf null. Das Repository übersetzt diesen Zustand in RemoveImage=true. Die
 *   sichere Reihenfolge Person ändern -> altes Serverbild löschen liegt danach
 *   vollständig im People-UseCase der WebAPI.
 *
 * - Beim erfolgreichen Speichern gilt:
 *
 *      Person validieren
 *          -> Repository.create/update(...)
 *          -> PeopleApi erfolgreich
 *          -> temporäre lokale Bilddatei löschen
 *          -> ShowMessage
 *          -> NavigateBack
 *
 * - Beim Abbrechen gilt:
 *
 *      Cancel
 *          -> nur temporäre lokale Bilddatei löschen
 *          -> persistierte Server-URL unverändert lassen
 *          -> NavigateBack
 *
 * Lernziele:
 *
 * - Lokale Dateiverantwortung von Server-Ressourcen unterscheiden.
 * - Multipart-Upload als Übergang von einer temporären lokalen Datei zu einer
 *   persistenten Server-Ressource verstehen.
 * - Fachliche Server-Orchestrierung nicht im Android-ViewModel duplizieren.
 */
