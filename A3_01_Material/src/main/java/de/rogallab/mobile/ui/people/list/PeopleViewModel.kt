package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// New version of PersonViewModel using updateState inline function
class PeopleViewModel(
   private val _stringProvider: IStringProvider,
   private val _repository: IPersonRepository
): ViewModel() {

   // StateFlow PeopleUiState
   private val _stateFlow = MutableStateFlow(PeopleUiState())
   val stateFlow = _stateFlow.asStateFlow()

   // Start the reactive observation pipeline
   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   // Job to observe the repository for changes to the list of people.
   private var _observeJob: Job? = null

   // Observe the repository for changes to the list of people and update the UI state accordingly.
   private fun observePeople() {
      _observeJob?.cancel()

      _observeJob = viewModelScope.launch {
         // Show the loading indicator until the first result arrives.
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }
         delay(1000) // simulate loading delay


         // Observe the repository for changes to the list of people.
         _repository.observeAll().collect { result: Result<List<Person>> ->

            result
               // Update the UI state with the list of people when the repository operation succeeded.
               .onSuccess { people ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(people = people)
                  }
                  Alog.d(TAG, "observePeople: people=${_stateFlow.value.people.size}")
               }

               // Show an error message when the repository operation failed.
               .onFailure { throwable ->
                  val error = _stringProvider.getString(R.string.error_people_observe)
                  Alog.e(TAG, error, throwable)
               }

            // Reset the loading flag regardless of success or failure.
            _stateFlow.update { state: PeopleUiState ->
               state.copy(isLoading = false)
            }
         }
      }
   }

   // Dispatcher: transform intent into an action
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> Unit    // navigate to create handled in the UI layer
         is PeopleIntent.Detail -> Unit // navigate to details handled in the UI layer
         is PeopleIntent.Remove -> remove(intent.person)
      }
   }

   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {
               val message = _stringProvider.getString(
                  R.string.message_person_removed, person.fullName)
               Alog.d(TAG, "remove success")
            }
            .onFailure {
               Alog.e(TAG, it.message ?: "Error in remove")
            }
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}