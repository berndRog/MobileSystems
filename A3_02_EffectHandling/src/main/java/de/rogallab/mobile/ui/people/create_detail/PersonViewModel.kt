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
import kotlin.coroutines.cancellation.CancellationException

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
      Alog.d(TAG, "loadPerson: $id")

      // Mark the attempt before launching so repeated retries cannot overlap it.
      _stateFlow.update { state: PersonUiState ->
         state.copy(isLoading = true, loadFailure = null)
      }
      viewModelScope.launch {
         try {
            // Keep the existing delay used to demonstrate loading.
            delay(1000)

            _repository.findById(id)
               .onSuccess { person ->
                  if (person != null) {
                     Alog.d(TAG, "_repository.findById.onSuccess: $person")
                     _stateFlow.update { state: PersonUiState ->
                        state.copy(person = person, loadFailure = null)
                     }
                  } else {
                     val error = _stringProvider.getString(R.string.error_person_not_found)
                     Alog.d(TAG, "_repository.findById.onSuccess (error): $error")
                     _stateFlow.update { state: PersonUiState ->
                        state.copy(loadFailure = PersonLoadFailure.NotFound(error))
                     }
                  }
               }
               .onFailure { throwable ->
                  if (throwable is CancellationException) throw throwable
                  val error = _stringProvider.getString(R.string.error_person_load)
                  Alog.e(TAG, error, throwable)
                  _stateFlow.update { state: PersonUiState ->
                     state.copy(loadFailure = PersonLoadFailure.Failed(error))
                  }
               }
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            val error = _stringProvider.getString(R.string.error_person_load)
            Alog.e(TAG, "Unexpected load failure", e)
            _stateFlow.update { state: PersonUiState ->
               state.copy(loadFailure = PersonLoadFailure.Failed(error))
            }
         }
         finally {
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
         PersonIntent.RetryLoad ->
            // A3_02 has no Back action, so NotFound can also be retried.
            if (!_isNew && !_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               loadPerson(_personId!!)
            }
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

      // Validate the complete entity before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         viewModelScope.launch {
            _effectDelegate.emit(PersonEffect.ShowError(error))
         }
         return
      }

      // Expose the active save to both the UI and the duplicate-save guard.
      _stateFlow.update { state: PersonUiState -> state.copy(isSaving = true) }

      viewModelScope.launch {
         try {
            // New entities are inserted, existing entities are updated.
            val result =
               if (_isNew) _repository.create(person)
               else _repository.update(person)

            result
               .onSuccess {
                  val message = _stringProvider.getString(R.string.message_person_saved, person.fullName)
                  _effectDelegate.emit(PersonEffect.ShowMessage(message))
               }
               .onFailure { throwable ->
                  Alog.e(TAG, "Save failed", throwable)
                  val error = _stringProvider.getString(R.string.error_person_save)
                  _effectDelegate.emit(PersonEffect.ShowError(error))
               }
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            Alog.e(TAG, "Unexpected save failure", e)
            _effectDelegate.emit(PersonEffect.ShowError(
               _stringProvider.getString(R.string.error_person_save)
            ))
         }
         finally {
            _stateFlow.update { state: PersonUiState ->
               state.copy(isSaving = false)
            }
         }
      }
   }

   // Cancels editing
   private fun cancel() {
      if (_stateFlow.value.isSaving) return

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
 * - Ladefehler bleiben im UI-State sichtbar. Save-Erfolg, Save-Fehler und
 *   Validierungsfehler werden als einmalige PersonEffects ausgegeben.
 *   NotFound bietet in A3_02 ebenfalls Retry, weil ein Backstack fehlt.
 *   isSaving sperrt weitere Save- und Cancel-Aktionen bis zum Ergebnis.
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
 * - A3_02 besitzt noch keinen Backstack. Speichern zeigt eine Meldung,
 *   Cancel setzt das Formular zurück.
 *
 * Lernziele:
 *
 * - State und einmalige Effects unterscheiden.
 * - IStringProvider für String-Ressourcen und bereits vorhandene Strings unterscheiden.
 * - Implementierungsdelegation mit "by" verstehen.
 * - Fehlerbehandlung von konkreter UI-Darstellung entkoppeln.
 * - Ladefehler als State und Fehler einzelner Aktionen als Effects behandeln.
 */
