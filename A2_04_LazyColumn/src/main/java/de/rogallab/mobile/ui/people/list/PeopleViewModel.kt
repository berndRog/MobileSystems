package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// New version of PersonViewModel using updateState inline function
class PeopleViewModel(
   private val _repository: IPersonRepository
): ViewModel() {

   // StateFlow PersonScreen
   private val _stateFlow: MutableStateFlow<PeopleUiState> = MutableStateFlow(PeopleUiState())
   val stateFlow: StateFlow<PeopleUiState> = _stateFlow.asStateFlow()


   // initialize the ViewModel by observing the repository for changes to the list of people.
   init {
      observePeople()
   }

   // Job to observe the repository for changes to the list of people.
   private var _observeJob: Job? = null

   // Observe the repository for changes to the list of people and update the UI state accordingly.
   private fun observePeople() {

      // Cancel any existing observation job before starting a new one.
      _observeJob?.cancel()

      // Start a new observation job to collect changes from the repository.
      _observeJob = viewModelScope.launch {

         // Show the loading indicator until the first result arrives.
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }

         // Observe the repository for changes to the list of people.
         _repository.observeAll().collect { result: Result<List<Person>> ->

            // pattern .onSuccess { people -> ... } .onFailure { ... } is used to handle the
            result
               // Repository operation succeeded: Update the UI state with the list of people
               .onSuccess { people ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(people = people, isLoading = false)
                  }
                  Alog.d(TAG, "observePeople ${_stateFlow.value.people.size}")
               }

               // Repository operation failed: Update the UI state to indicate that loading
               // has finished and show an error message.
               .onFailure {
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }
                  Alog.e(TAG, "observePeople failed")
               }
         }
      }
   }

   // Dispatcher: transform intent into an action
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "onIntent: $intent")
      when (intent) {
         is PeopleIntent.OpenDetail ->
            Alog.d(TAG, "OpenDetail: Navigate to DetailScreen")
         is PeopleIntent.Remove ->
            remove(intent.person)
      }
   }

   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {  }
            .onFailure {
               Alog.e(TAG, it.message ?: "Error in remove")
            }
      }
   }



   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}