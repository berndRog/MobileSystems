package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
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
   private val _imageEditDelegate: ImageEditDelegate,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null
   private var _isSaving = false

   private val _initialState =
      if (_isNew)
         PersonUiState(isNew = true, isLoading = false)
      else
         PersonUiState(isNew = false, isLoading = true)

   // Holds the persistent UI state of the person screen.
   private val _stateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(_initialState)

   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   // Loads an existing person from the repository.
   private fun loadPerson(id: String) {
      viewModelScope.launch {
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         delay(2500)

         _repository.findById(id)
            .onSuccess { person ->
               if (person == null) {
                  Alog.e(TAG, "Person not found: $id")
                  _effectDelegate.emit(
                     PersonEffect.ShowError(
                        _stringProvider.getString(R.string.error_person_not_found)
                     )
                  )

                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               _imageEditDelegate.start(person.imagePath)

               _stateFlow.update { state: PersonUiState ->
                  state.copy(
                     person = person,
                     isLoading = false,
                  )
               }
            }
            .onFailure { throwable ->
               Alog.e(TAG, "loadPerson failed: ${throwable.message}")
               _effectDelegate.emit(
                  PersonEffect.ShowError(
                     _stringProvider.getString(R.string.error_person_load)
                  )
               )

               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   // Dispatches incoming UI intents to the corresponding action.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
         is PersonIntent.ImagePathChange -> changeImage(intent.imagePath)
         is PersonIntent.ImageStorageFailed -> showError(intent.message)
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(firstName = firstName.trim())
         )
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(lastName = lastName.trim())
         )
      }

   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(email = emailNullable)
         )
      }
   }

   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(phone = phoneNullable)
         )
      }
   }

   // Delegates the lifetime of selected image files to shared_01. The
   // PersonUiState remains the only StateFlow observed by the screen.
   private fun changeImage(imagePath: String?) {
      viewModelScope.launch {
         val newImagePath = _imageEditDelegate.replace(imagePath)
         _stateFlow.update { state: PersonUiState ->
            state.copy(person = state.person.copy(imagePath = newImagePath))
         }
      }
   }

   // Converts an image-storage failure into the same UI error path used by
   // repository and validation errors.
   private fun showError(message: String) {
      viewModelScope.launch {
         _effectDelegate.emit(
            PersonEffect.ShowError(message)
         )
      }
   }

   // Validates and persists the current person.
   private fun save() {
      if (_isSaving) return

      var person = _stateFlow.value.person.normalized()

      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(
                  person = state.person.copy(email = email)
               )
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(
                  person = state.person.copy(phone = phone)
               )
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      val errorMessage = _validator.validatePerson(person)
      if (errorMessage != null) {
         Alog.e(TAG, errorMessage)
         showError(errorMessage)
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
               // The database owns the new selection only after Save succeeds.
               _imageEditDelegate.commit()

               _effectDelegate.emit(
                  PersonEffect.ShowMessage(
                     _stringProvider.getString(R.string.message_person_saved, person.fullName)
                  )
               )

               _effectDelegate.emit(
                  PersonEffect.NavigateBack(BackReason.Save)
               )
            }
            .onFailure { throwable ->
               Alog.e(TAG, "save failed: ${throwable.message}")
               _effectDelegate.emit(
                  PersonEffect.ShowError(
                     _stringProvider.getString(R.string.error_person_save)
                  )
               )
            }

         _isSaving = false
      }
   }

   // Discards newly selected image files and requests back navigation.
   private fun cancel() {
      viewModelScope.launch {
         _imageEditDelegate.discard()
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
 * - A3_05 übernimmt die Bildbearbeitung aus A3_04 unverändert. Die neuen
 *   Swipe-Gesten betreffen ausschließlich den PeopleListScreen.
 *
 * - ImagePickerHandler speichert ausgewählte Galerie-Bilder und Kamera-Fotos
 *   zuerst in den privaten App-Speicher. Das ViewModel erhält anschließend nur
 *   den gespeicherten Dateipfad über PersonIntent.ImagePathChange.
 *
 * - Die Lebensdauer dieser Dateien wird an den gemeinsamen ImageEditDelegate
 *   delegiert. PersonViewModel kennt dadurch weder Original-/Ersatzbild-Logik
 *   noch direkte Datei-Löschoperationen.
 *
 * - Der Screen beobachtet weiterhin genau einen StateFlow<PersonUiState>. Der
 *   Delegate verwaltet nur internen Bearbeitungszustand und keinen zweiten Flow.
 *   Auch der reine Schreibschutz während Save bleibt als private ViewModel-Variable
 *   außerhalb des UI-States, da der Screen ihn nicht darstellt.
 *
 * - Fehler des ImagePicker werden über ImageStorageFailed in denselben
 *   ShowError-Effect übersetzt wie andere Fehler des Features.
 *
 * - State-Änderungen verwenden weiterhin konsequent:
 *
 *      _stateFlow.update { state: PersonUiState ->
 *         state.copy(...)
 *      }
 *
 * Lernziele:
 *
 * - Activity-Result-basierte Bildauswahl in eine MVI-Struktur integrieren.
 * - Galerie und Kamera hinter einer gemeinsamen Komponente kapseln.
 * - Bilddateien über Delegation statt ViewModel-Vererbung verwalten.
 * - Einzel- und Mehrfachbilder mit derselben Shared-Komponente vorbereiten.
 * - Bestehendes Effect- und Snackbar-Handling für neue Fehler wiederverwenden.
 */
