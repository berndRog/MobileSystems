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

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator
): ViewModel() {

   private val _personId = personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null
   private var _isSaving = false

   // Initial state depends on whether this instance creates or edits a person.
   private val _initialState =
      if (_isNew)
         PersonUiState(isNew = true, isLoading = false)
      else
         PersonUiState(isNew = false, isLoading = true)


   // StateFlow PersonScreen
   private val _stateFlow: MutableStateFlow<PersonUiState> = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<PersonUiState> = _stateFlow.asStateFlow()

   // For a detail screen of an existing person,
   // load the person data when the ViewModel is initialized.
   init {
      // Detail screen for existing person, load the person data.
      if(!_isNew) loadPerson(_personId!!)
   }

   private fun loadPerson(id: String) {
      viewModelScope.launch {

         // isLoading = true
         _stateFlow.update { state: PersonUiState ->
            state.copy(isLoading = true)
         }

         delay(2500) // simulate loading delay

         // load person by id from repository
         _repository.findById(id)
            // Result<Person?> is returned
            .onSuccess { person ->
               // person can be null if not found
               if (person == null) {
                  var error = _stringProvider.getString(R.string.error_person_not_found)
                  Alog.e(TAG, error)
                  _stateFlow.update { state: PersonUiState ->
                     state.copy(isLoading = false)
                  }
                  return@onSuccess
               }
               // update the state with the fetched person
               _stateFlow.update { state: PersonUiState ->
                  state.copy(isLoading = false, person = person)
               }
            }
            .onFailure {
               var error = _stringProvider.getString(R.string.error_person_load)
               Alog.e(TAG, error)
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
      if(email.trim().isNotEmpty()) emailNullable = email.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(email = emailNullable))
      }
   }

   private fun changePhone(phone: String) {
      var phoneNullable: String? = null
      if(phone.trim().isNotEmpty()) phoneNullable = phone.trim()
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(phone = phoneNullable))
      }
   }

   // Performs final validation and persists the person in this feature
   // ViewModel. The coordinator is used only for displaying the result message.
   private fun save() {
      if (_isSaving) return

      var person = _stateFlow.value.person.normalized()

      // sanitized email address ä,ö,ü-> ae,oe,ue etc.
      if(person.email != null) {
         val email = sanitizeEmailInput(person.email)
         if(email != person.email) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(email = email))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // sanitized phone, leave only valid characters
      if(person.phone != null) {
         val phone = sanitizePhoneInput(person.phone)
         if(phone != person.phone) {
            _stateFlow.update { state: PersonUiState ->
               state.copy(person = state.person.copy(phone = phone))
            }
            person = _stateFlow.value.person.normalized()
         }
      }

      // Validate the person data before saving.
      val errorMessage = _validator.validatePerson(person)
      if (errorMessage != null) {
         //showSnackbar(errorMessage)
         Alog.e(TAG, errorMessage)
         return
      }

      // Update: save operation is in progress.
      _isSaving = true

      viewModelScope.launch {
         // If _isNew is true, create a new person;
         // otherwise, update the existing person.
         val result =
            if (_isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               var message = _stringProvider.getString(R.string.message_person_saved, person.fullName)
               Alog.i(TAG, message)
            }
            .onFailure {
               var error = _stringProvider.getString(R.string.error_person_save)
               Alog.e(TAG, error)
            }

         // Update: save operation is finished.
         _isSaving = false

      }
   }

   private fun cancel() {
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = "", lastName = ""))
      }
   }


   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}