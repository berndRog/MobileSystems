package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.R as SharedR
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _imageFileStorage: IImageFileStorage,
   private val _imageEdit: IImageEdit,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // Normalize the optional navigation parameter.
   // A null or blank id means that a new person is being created.
   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null

   // Prevent duplicate repository writes while a Save operation is running.
   // This internal processing flag is not part of the observable UI state.
   private var _isSaving = false

   // A new person can be edited immediately, while an existing person
   // has to be loaded from the repository first.
   private val _initialState =
      if (_isNew)
         PersonUiState(isNew = true, isLoading = false)
      else
         PersonUiState(isNew = false, isLoading = true)

   // Mutable state stays private inside the ViewModel.
   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)

   // The UI observes only the read-only StateFlow.
   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   init {
      // Existing persons are loaded automatically when the ViewModel is created.
      if (!_isNew) loadPerson(_personId!!)
   }

   // Load an existing person and initialize the image edit session with the
   // image path that is currently persisted by the entity.
   private fun loadPerson(id: String) {
      viewModelScope.launch {
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         // Keep the longer A3_05 teaching delay used by this example.
         delay(2500)

         _repository.findById(id)
            .onSuccess { person ->
               if (person == null) {
                  Alog.e(TAG, "Person not found: $id")
                  val error = _stringProvider.getString(R.string.error_person_not_found)

                  // Errors are one-time effects and are not stored in PersonUiState.
                  _effectDelegate.emit(PersonEffect.ShowError(error))
                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               // Remember the persisted image as the original edit-session image.
               _imageEdit.start(listOfNotNull(person.imagePath))

               // Publish the loaded person as persistent UI state.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person, isLoading = false)
               }
            }
            .onFailure { throwable ->
               Alog.e(TAG, "loadPerson failed: ${throwable.message}")
               val error = _stringProvider.getString(R.string.error_person_load)
               _effectDelegate.emit(PersonEffect.ShowError(error))
               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   // Single public entry point for all UI events.
   // Each intent is forwarded to a private ViewModel action.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)

         // Gallery images still arrive as external Content-URIs.
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)

         // Camera images already arrive as internal paths. A null path removes
         // the image from the current edit-session selection.
         is PersonIntent.ImagePathChange -> changeImage(intent.imagePath)

         // Technical image failures use the same one-time error effect.
         is PersonIntent.ImageStorageFailed -> showError(intent.message)

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

   // Empty input fields are represented as null in the domain entity.
   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   // Empty input fields are represented as null in the domain entity.
   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // Copy a Photo Picker Content-Uri into private app storage before the image
   // becomes part of the current edit session.
   private fun storeGalleryImage(sourceUri: Uri) {
      viewModelScope.launch {
         val imagePath = _imageFileStorage
            .copyImageToAppStorage(sourceUri)
            .getOrElse {
               // Keep the current image unchanged when copying fails.
               showError(_stringProvider.getString(SharedR.string.error_image_save))
               return@launch
            }

         // From this point on the feature works only with an internal file path.
         replaceImage(imagePath)
      }
   }

   // Camera images already arrive as confirmed internal paths.
   private fun changeImage(imagePath: String?) {
      viewModelScope.launch {
         replaceImage(imagePath)
      }
   }

   // Delegate image lifecycle decisions to IImageEdit. The ViewModel only
   // supplies the desired selection and publishes the resulting image path.
   private suspend fun replaceImage(imagePath: String?) {
      // Person currently supports one image, while IImageEdit uses a list so
      // that the same delegate can later support entities with multiple images.
      val images = _imageEdit.replace(listOfNotNull(imagePath))

      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(imagePath = images.firstOrNull())
         )
      }
   }

   // Convert an error into a one-time UI effect.
   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(PersonEffect.ShowError(message))
      }
   }

   // Validate and persist the current person.
   private fun save() {
      // Ignore additional Save requests while a repository write is active.
      if (_isSaving) return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // Sanitize the email and publish the sanitized value back to the UI.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Sanitize the phone number and publish the sanitized value back to the UI.
      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Validate the complete entity before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         Alog.e(TAG, error)
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
               // Only after the database write succeeds may persisted original
               // images that are no longer selected be deleted.
               _imageEdit.commit()

               val message = _stringProvider.getString(
                  R.string.message_person_saved,
                  person.fullName,
               )
               _effectDelegate.emit(PersonEffect.ShowMessage(message))
               _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
            }
            .onFailure { throwable ->
               Alog.e(TAG, "save failed: ${throwable.message}")

               // Do not commit the image session after a failed database write.
               // Original and replacement images remain available for retry/cancel.
               val errorMessage = _stringProvider.getString(R.string.error_person_save)
               _effectDelegate.emit(PersonEffect.ShowError(errorMessage))
            }

         _isSaving = false
      }
   }

   // Discard the edit session before leaving without saving.
   private fun cancel() {
      viewModelScope.launch {
         // Remove only images created during this edit session. Persisted
         // originals remain untouched.
         _imageEdit.discard()

         // Navigate only after the image cleanup has completed.
         _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Cancel))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_05 übernimmt die Bildbearbeitung aus A3_04. Die Swipe-Gesten von A3_05
 *   betreffen den PeopleListScreen; PersonViewModel bleibt für die Bearbeitung
 *   genau einer Person zuständig.
 *
 * - PersonViewModel verarbeitet alle UI-Ereignisse über onIntent(...). Der
 *   persistente Zustand wird über StateFlow<PersonUiState> ausgegeben, während
 *   Meldungen, Fehler und Navigation weiterhin einmalige PersonEffects sind.
 *
 * - Galerie und Kamera liefern unterschiedliche Ausgangsdaten:
 *
 *      Galerie  -> Content-Uri
 *      Kamera   -> bestätigter interner Dateipfad
 *
 * - Eine Galerie-Uri wird deshalb zunächst über IImageFileStorage in den
 *   privaten App-Speicher kopiert:
 *
 *      GalleryPickerHandler
 *          -> Uri
 *          -> GalleryImageSelected
 *          -> PersonViewModel
 *          -> IImageFileStorage.copyImageToAppStorage(...)
 *          -> imagePath
 *          -> IImageEdit.replace(...)
 *          -> PersonUiState
 *
 * - Kamera-Bilder erreichen das ViewModel bereits als interner Dateipfad und
 *   können direkt an IImageEdit weitergegeben werden.
 *
 * - IImageFileStorage übernimmt technische Dateioperationen. IImageEdit verwaltet
 *   dagegen die Lebensdauer der Bilder innerhalb einer Edit-Session. Dadurch
 *   kennt PersonViewModel nicht die einzelnen Regeln zum Löschen von Original-
 *   und Ersatzbildern.
 *
 * - Beim Laden einer bestehenden Person startet IImageEdit mit dem persistenten
 *   Originalbild. replace(...) ändert die aktuelle Auswahl, ohne ein persistiertes
 *   Original vor einem erfolgreichen Save zu löschen.
 *
 * - Erst nach erfolgreichem Repository-Zugriff wird commit() ausgeführt:
 *
 *      Repository.create/update(...)
 *          -> erfolgreich
 *          -> IImageEdit.commit()
 *          -> ShowMessage
 *          -> NavigateBack(Save)
 *
 * - Beim Abbruch wird discard() vor der Navigation aufgerufen. Neu erzeugte,
 *   nicht persistierte Bilder werden entfernt; Originalbilder bleiben erhalten.
 *
 * - _isSaving ist nur technischer interner Zustand und bleibt außerhalb von
 *   PersonUiState, weil der Screen diesen Zustand nicht darstellt.
 *
 * - Die Swipe-/Undo-Logik von A3_05 bleibt von diesem Edit-Workflow unabhängig
 *   und wird weiterhin im PeopleViewModel bzw. PeopleListScreen behandelt.
 *
 * Lernziele:
 *
 * - Einen einzigen Intent-Einstiegspunkt im ViewModel verwenden.
 * - State und einmalige Effects voneinander unterscheiden.
 * - Content-Uri und internen Dateipfad unterscheiden.
 * - Galerie-Bilder vor der Nutzung in den privaten App-Speicher kopieren.
 * - Technische Dateiverwaltung über IImageFileStorage kapseln.
 * - Bild-Lebenszyklen einer Bearbeitung über IImageEdit delegieren.
 * - commit() erst nach erfolgreichem Repository-Zugriff ausführen.
 * - discard() zum Aufräumen einer abgebrochenen Edit-Session verwenden.
 */
