package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
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
   private val _validator: PersonValidator
) : ViewModel() {

   // A null or blank id means that a new person is created.
   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null
   private var _isSaving = false

   // Initial state depends on whether this instance creates or edits a person.
   private val _initialState =
      if (_isNew)
         PersonUiState(isNew = true, isLoading = false)
      else
         PersonUiState(isNew = false, isLoading = true)

   // Mutable PersonUiState is used only internally within the ViewModel.
   private val _stateFlow: MutableStateFlow<PersonUiState> = MutableStateFlow(_initialState)
   // Exposes the PersonUiState as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PersonUiState> =
      _stateFlow.asStateFlow()

   // For a detail screen load the existing person.
   init {
      if (!_isNew) loadPerson(_personId!!)
   }

   // Load existing person in detail mode.
   private fun loadPerson(id: String) {
      // isLoading = true
      _stateFlow.update { state: PersonUiState ->
         state.copy(isLoading = true)
      }

      viewModelScope.launch {
         try {
            delay(2500) // simulate loading delay

            // load person by id from repository
            _repository.findById(id)   // Result<Person?> is returned

               .onSuccess { person ->
                  // Store the loaded person.
                  if (person != null) {
                     Alog.d(TAG, "_repository.findById.onSuccess: $person")
                     _stateFlow.update { state: PersonUiState ->
                        state.copy(person = person)
                     }
                  }
                  // A successful Result may still contain null if no person exists.
                  else {
                     val error = _stringProvider.getString(R.string.error_person_not_found)
                     Alog.d(TAG, "_repository.findById.onSuccess (error): $error")
                  }
               }

               // Handle repository errors
               .onFailure { throwable ->
                  // Rethrow CancellationException to allow coroutine cancellation to propagate
                  if (throwable is CancellationException) throw throwable
                  var error = _stringProvider.getString(R.string.error_person_load)
                  Alog.e(TAG, error)
               }
         }

         // Handle unexpected exceptions in coroutines.
         catch (e: CancellationException) {
            throw e
         }

         // Handle unexpected exceptions
         catch (e: Exception) {
            var error = _stringProvider.getString(R.string.error_person_load)
            Alog.e(TAG, "$error: ${e.message}")
         }

         // Reset the loading flag regardless of success or failure.
         finally {
            _stateFlow.update { state: PersonUiState ->
               state.copy(isLoading = false)
            }
         }
      }
   }

   // Dispatcher: transform intent into an action
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
         is PersonIntent.Save -> save()
         is PersonIntent.Cancel -> cancel()
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

   // Validates and persists the current person.
   private fun save() {

      // Prevent multiple concurrent save operations
      if (_stateFlow.value.isSaving || _stateFlow.value.isLoading)
         return

      // Normalize all form values before validation and persistence.
      var person = _stateFlow.value.person.normalized()

      // sanitized email address ä,ö,ü-> ae,oe,ue etc.
      if (person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if (email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // sanitized phone, leave only valid characters
      if (person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if (phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Validate the complete person before accessing the repository.
      val errorMessage = _validator.validatePerson(person)
      if (errorMessage != null) {
         Alog.e(TAG, errorMessage)
         return
      }

      // Expose the active save to both the UI and the duplicate-save guard.
      _stateFlow.update { state: PersonUiState ->
         state.copy(isSaving = true)
      }

      viewModelScope.launch {

         try {

            // Create a new person or update the existing person
            val result =
               if (_isNew) _repository.create(person)
               else _repository.update(person)

            result
               // Handle the repository result and emit success
               .onSuccess {
                  var message = _stringProvider.getString(R.string.message_person_saved, person.fullName)
                  Alog.i(TAG, message)
               }
               // Handle repository errors and emit a one-time effect for failure
               .onFailure {
                  var error = _stringProvider.getString(R.string.error_person_save)
                  Alog.e(TAG, error)
               }

         }

         // Handle unexpected exceptions in coroutines.
         catch (e: CancellationException) {
            Alog.e(TAG, "Unexpected cancellation", e)
            throw e
         }

         // Handle unexpected exceptions and emit a one-time effect for failure.
         catch (e: Exception) {
            Alog.e(TAG, "Unexpected save failure", e)

         }

         // Reset the saving flag regardless of success or failure.
         finally {
            _stateFlow.update { state: PersonUiState ->
               state.copy(isSaving = false)
            }
         }
      }
   }

   private fun cancel() {

      if (_stateFlow.value.isSaving) return

      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = "", lastName = ""))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}