package de.rogallab.mobile.ui.people.create_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// New version of PersonViewModel using updateState inline function
class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository
): ViewModel() {

   // personId determines whether this instance creates or edits a person.
   private val _personId = personId
      ?.takeUnless(String::isBlank)

   // isNew is true indicates a new person creation.
   private val _isNew = _personId == null

   // Initial state depends on whether this instance creates or edits a person.
   private val _initialState =
      if (_isNew)
         PersonUiState(person = Person(), isNew = true)  // New person creation
      else
         PersonUiState(isNew = false, isLoading = true)  // Edit existing person, load state

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
         _repository.findById(id)
            // Result<Person?> is returned
            .onSuccess { person ->
               // person can be null if not found
               if (person == null) {
                  Alog.e(TAG, "Person not found: $id", )
                  return@onSuccess
               }
               // update the state with the fetched person
               _stateFlow.update { state: PersonUiState ->
                  state.copy(person = person)
               }
               Alog.d(TAG, "load(): ${stateFlow.value.person}")
            }
            .onFailure {
               Alog.e(TAG, it.message ?: "Error in load")
            }
      }
   }

   // Dispatcher: transform intent into an action
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)

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

   private fun save() {
      viewModelScope.launch {
         var person = _stateFlow.value.person

         val result =
            if (_isNew) _repository.create(person)
            else _repository.update(person)

         result
            .onSuccess {
               Alog.d(TAG, "save success: ${stateFlow.value.person}")
            }
            .onFailure {
               Alog.e(TAG, it.message ?: "Error in save")
            }
      }
   }

   fun cancel() {
      Alog.d(TAG, "cancel()")
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = "", lastName = ""))
      }
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}