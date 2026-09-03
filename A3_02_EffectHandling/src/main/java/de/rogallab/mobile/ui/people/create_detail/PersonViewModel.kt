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
   private val _effectDelegate: EffectDelegate<PersonEffect>,
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // A null or blank id means that a new person is created.
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

   // Load the existing person when the ViewModel is created in edit mode.
   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   // Load the existing person when the ViewModel is detail mode.
   private fun loadPerson(id: String) {
      viewModelScope.launch {
         // Indicate that the loading operation is in progress.
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         // Simulate a longer loading operation.
         delay(1000)

         // Find the person by id.
         _repository.findById(id)
            .onSuccess { person ->
               // A successful Result may still contain null if no person exists.
               if (person == null) {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  _effectDelegate.emit(PersonEffect.ShowError(error))

                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }

               val message = "Test: ${person.firstName} ${person.lastName} geladen"
               _effectDelegate.emit(PersonEffect.ShowMessage(message))

               // Store the loaded person and finish the loading operation.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person, isLoading = false)
               }
            }
            .onFailure { throwable ->
               // Repository failures are converted into a localized UI effect.
               val error = _stringProvider.getString(R.string.error_person_load)
               _effectDelegate.emit(PersonEffect.ShowError(error))

               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false)
               }
            }
      }
   }

   // Dispatcher: Single public entry point for all events coming from the UI layer.
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
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

   // Updates the optional email address in the current UI state.
   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   // Updates the optional phone number in the current UI state.
   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if (phone.trim().isNotEmpty()) phoneNullable = phone.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // Validates and persists the current person.
   private fun save() {

      // Prevent multiple concurrent save operations.
      if (_isSaving) return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // Sanitize the email address before final validation.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Sanitize the phone number before final validation.
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
         viewModelScope.launch {
            _effectDelegate.emit(PersonEffect.ShowError(error))
         }
         return
      }

      // Publish the normalized and validated person before saving it.
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = person)
      }

      // Update: save operation is in progress.
      _isSaving = true

      viewModelScope.launch {

         // New entities are inserted, existing entities are updated.
         val result =
            if (_isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               // First show the success message...
               val message = _stringProvider.getString(R.string.message_person_saved, person.fullName)
               _effectDelegate.emit(PersonEffect.ShowMessage(message))
            }
            .onFailure { throwable ->
               val error = _stringProvider.getString(R.string.error_person_save)
               _effectDelegate.emit(PersonEffect.ShowError(error))
            }

         // Update: save operation is finished
         _isSaving = false
      }
   }

   // Cancels editing
   private fun cancel() {
      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(firstName = "",lastName = "")
         )
      }

   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * Dieses ViewModel verwaltet zwei unterschiedliche Informationsarten:
 * dauerhaften UI-State und einmalige UI-Effects.
 *
 * - PersonUiState enthält den dauerhaft sichtbaren Zustand des Screens.
 *   Änderungen erfolgen konsequent in der Form:
 *
 *      _stateFlow.update { state: PersonUiState ->
 *         state.copy(...)
 *      }
 *
 *   Dadurch ist bereits am Lambda-Bezeichner erkennbar, welcher State
 *   verändert wird.
 *
 * - Meldungen, Fehler und vorbereitete Navigation werden als PersonEffect
 *   ausgegeben und nicht im dauerhaften State gespeichert.
 *
 * - Bekannte Texte werden über IStringProvider aufgelöst und als String transportiert:
 *
 *      _stringProvider.getString(R.string.error_person_save)
 *
 * - Bereits vorhandene Strings, beispielsweise aus der Validierung, werden
 *   direkt in den Effect übernommen:
 *
 *      errorMessage
 *
 * - Das ViewModel benötigt keinen Context. String-Ressourcen werden über
 *   IStringProvider bereits hier in fertige Strings aufgelöst.
 *
 * - IEffectSource<PersonEffect> wird mit "by _effectDelegate" delegiert.
 *   Channel und Flow müssen deshalb nicht in jedem ViewModel erneut
 *   implementiert werden.
 *
 * - NavigateBack ist bereits vollständig im Effect-Typ vorbereitet. In diesem
 *   Lernschritt wird der Callback noch nicht mit einem Back Stack verbunden.
 *   Save und Cancel liefern unterschiedliche BackReason-Werte, damit der
 *   nächste Navigationsschritt unterschiedliche Animationen zeigen kann.
 *
 * Lernziele:
 *
 * - State und einmalige Effects unterscheiden.
 * - IStringProvider für String-Ressourcen und bereits vorhandene Strings unterscheiden.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Fehlerbehandlung von konkreter UI-Darstellung entkoppeln.
 * - Navigation vorbereiten, ohne sie bereits funktional einzuführen.
 */
