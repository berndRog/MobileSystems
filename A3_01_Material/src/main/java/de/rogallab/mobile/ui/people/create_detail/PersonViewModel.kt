package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.normalized
import de.rogallab.mobile.ui.people.toNullableInput
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
   private val _personId = personId?.toNullableInput()

   // The initial state depends on whether a person is created or edited.
   private val _initialState =
      if (_personId == null) PersonUiState(isNew = true,  isLoading = false)
      else                   PersonUiState(isNew = false, isLoading = true)

   // StateFloe PersonUiState
   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow = _stateFlow.asStateFlow()

   // For a detail screen load the existing person.
   init {
      if (!_stateFlow.value.isNew) loadPerson(_personId!!)
   }

   // Load existing person in detail mode.
   private fun loadPerson(id: String) {

      // Mark the attempt before launching so repeated retries cannot overlap it.
      _stateFlow.update { state -> state.copy(isLoading = true) }

      viewModelScope.launch {

         delay(1000) // simulate loading delay

         // load person by id from repository
         _repository.findById(id)   // Result<Person?> is returned

            // Handle successful repository result
            .onSuccess { person ->
               // Store the loaded person.
               if (person != null) {
                  Alog.d(TAG, "_repository.findById.onSuccess: $person")
                  _stateFlow.update { state -> state.copy(person = person) }
               }
               // A successful Result may still contain null if no person exists.
               else {
                  val error = _stringProvider.getString(R.string.error_person_not_found)
                  Alog.d(TAG, "_repository.findById.onSuccess (error): $error")
               }
            }

            // Handle repository failure returned as Result.failure
            .onFailure { throwable ->
               // Rethrow CancellationException to allow coroutine cancellation to propagate
               if (throwable is CancellationException) throw throwable
               var error = _stringProvider.getString(R.string.error_person_load)
               Alog.e(TAG, error)
            }

         // Reset the loading flag regardless of success or failure.
         _stateFlow.update { state -> state.copy(isLoading = false) }
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
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(firstName = firstName))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(lastName = lastName))
      }

   private fun changeEmail(email: String) {
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(email = email.toNullableInput()))
      }
   }

   private fun changePhone(phone: String) {
      _stateFlow.update { state ->
         state.copy(person = state.person.copy(phone = phone.toNullableInput()))
      }
   }

   // Validates and persists the current person.
   private fun save() {

      // Prevent multiple concurrent save operations
      if (_stateFlow.value.isSaving || _stateFlow.value.isLoading)
         return

      // Publish the normalized snapshot even when validation fails.
      val person = _stateFlow.value.person.normalized()
      _stateFlow.update { state -> state.copy(person = person) }

      // Validate the complete person before accessing the repository.
      val error = _validator.validatePerson(person)
      if (error != null) {
         Alog.d(TAG, "error: $error")
         return
      }

      // Expose the active save to both the UI and the duplicate-save guard.
      _stateFlow.update { state -> state.copy(isSaving = true) }

      viewModelScope.launch {

         // Create a new person or update the existing person
         val result =
            if (_stateFlow.value.isNew) _repository.create(person)
            else                        _repository.update(person)

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

         // Reset the saving flag regardless of success or failure.
         _stateFlow.update { state  -> state.copy(isSaving = false) }
      }
   }

   private fun cancel() {

      if (_stateFlow.value.isSaving) return

      _stateFlow.update { state ->
         state.copy(person = state.person.copy(firstName = "", lastName = ""))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}