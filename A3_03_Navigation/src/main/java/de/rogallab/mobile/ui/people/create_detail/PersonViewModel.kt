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
import kotlinx.coroutines.Job
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

   // A null or blank id means that a new person is being created.
   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null

   // Leaving invalidates results even if a repository does not cooperate with cancellation.
   private var _operationGeneration = 0
   private var _loadJob: Job? = null
   private var _saveJob: Job? = null

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

   // Loads an existing person from the repository.
   private fun loadPerson(id: String) {
      // Clear the previous failure before launching; repeated RetryLoad taps
      // cannot start parallel loads while this attempt is in progress.
      _stateFlow.update { state: PersonUiState ->
         state.copy(isLoading = true, loadFailure = null)
      }
      _loadJob?.cancel()
      val generation = _operationGeneration
      _loadJob = viewModelScope.launch {
         // Simulate a longer loading operation.
         delay(1000)

         // Find the person by id.
         val result = _repository.findById(id)
         if (generation != _operationGeneration) {
            result.exceptionOrNull()?.let { Alog.e(TAG, "Ignored load failure after leaving", it) }
            return@launch
         }
         result
            .onSuccess { person ->

               // A successful Result may still contain null if no person exists.
               if (person == null) {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  Alog.e(TAG, error)
                  _stateFlow.update { state: PersonUiState ->
                     state.copy(loadFailure = PersonLoadFailure.NotFound(error))
                  }
                  return@onSuccess
               }

               // Store the loaded person.
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person, loadFailure = null)
               }
            }
            .onFailure { throwable ->
               // Loading failures remain visible until a retry succeeds or the user leaves.
               val error = _stringProvider.getString(R.string.error_person_load)
               Alog.e(TAG, error, throwable)
               _stateFlow.update { state: PersonUiState ->
                  state.copy(loadFailure = PersonLoadFailure.Failed(error))
               }
            }

         // set isLoading = false after loading is complete
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = false)
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
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
         PersonIntent.RetryLoad ->
            if (_stateFlow.value.loadFailure is PersonLoadFailure.Failed) loadPerson(_personId!!)
      }
   }

   val canLeaveScreen: Boolean
      get() = !_stateFlow.value.isSaving

   // Called before removing this destination from the back stack.
   fun onScreenLeft() {
      _operationGeneration++
      _loadJob?.cancel()
      _saveJob?.cancel()
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

   // Updates the optional email address while keeping the current UI state.
   private fun changeEmail(email: String) {
      var emailNullable: String? = null
      if (email.trim().isNotEmpty()) emailNullable = email.trim()

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   // Updates the optional phone number while keeping the current UI state.
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
      if (_stateFlow.value.isSaving || _stateFlow.value.isLoading ||
         _stateFlow.value.loadFailure != null) return

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

      // Perform final validation before writing to the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         viewModelScope.launch {
            _effectDelegate.emit(PersonEffect.ShowError(error))
         }
         return
      }

      // Update: save operation is in progress.
      _stateFlow.update { state: PersonUiState -> state.copy(isSaving = true) }

      val generation = _operationGeneration
      _saveJob = viewModelScope.launch {
         try {
            // New entities are inserted, existing entities are updated.
            val result =
               if (_isNew) _repository.create(person)
               else _repository.update(person)
            if (generation != _operationGeneration) {
               result.exceptionOrNull()?.let { Alog.e(TAG, "Ignored save failure after leaving", it) }
               return@launch
            }

            result
               .onSuccess {
                  val message = _stringProvider.getString(R.string.message_person_saved, person.fullName)
                  _effectDelegate.emit(PersonEffect.ShowMessage(message))

                  // The adapter translates this effect into a Navigation 3 operation.
                  _effectDelegate.emit(PersonEffect.NavigateBack(BackReason.Save))
               }
               .onFailure { throwable ->
                  val error = _stringProvider.getString(R.string.error_person_save)
                  _effectDelegate.emit(PersonEffect.ShowError(error))
               }
         } finally {
            _stateFlow.update { state: PersonUiState -> state.copy(isSaving = false) }
         }
      }
   }

   // Cancels editing and emits the prepared back-navigation effect.
   private fun cancel() {
      if (!canLeaveScreen) return
      _stateFlow.update { state: PersonUiState ->
         state.copy(
            person = state.person.copy(firstName = "", lastName = "")
         )
      }

      // Navigation is emitted
      viewModelScope.launch {
         // The adapter translates this effect into a Navigation 3 operation.
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
 * - Fehler beim Laden bleiben als loadFailure im State sichtbar und bieten
 *   je nach Ursache Rücknavigation oder einen erneuten Ladeversuch. Fehler bei
 *   einzelnen Aktionen und vorbereitete Navigation bleiben einmalige Effects.
 *   Verlassen entwertet laufende Leseoperationen; während Save bleibt der
 *   Screen geöffnet, bis Erfolg oder Fehler feststeht.
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
 *   Lernschritt wird der Effect im Adapter an den Navigation-3-Back-Stack
 *   weitergegeben. Save und Cancel liefern unterschiedliche BackReason-Werte,
 *   damit unterschiedliche Rückwärtsanimationen sichtbar werden.
 *
 * Lernziele:
 *
 * - State und einmalige Effects unterscheiden.
 * - IStringProvider für String-Ressourcen und bereits vorhandene Strings unterscheiden.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Fehlerbehandlung von konkreter UI-Darstellung entkoppeln.
 * - Navigationseffects in konkrete Navigation-3-Operationen überführen.
 */
