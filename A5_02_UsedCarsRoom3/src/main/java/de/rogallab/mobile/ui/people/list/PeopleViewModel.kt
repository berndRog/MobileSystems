package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.VisualRemovalController
import de.rogallab.mobile.ui.common.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
) : ViewModel() {

   private val _state = MutableStateFlow(PeopleUiState())
   val state: StateFlow<PeopleUiState> = _state.asStateFlow()

   private val _events = Channel<PeopleEfect>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   private var _observeJob: Job? = null
   private val _visualRemoval = VisualRemovalController<Person>(Person::id)

   init {
      AppLogger.info(TAG, "init: observePeople()")
      observePeople()
   }

   fun onIntent(intent: PeopleIntent) {
      AppLogger.debug(TAG, "onIntent: $intent")

      when (intent) {
         PeopleIntent.Create -> emitEvent(PeopleEfect.NavigateToCreate)
         is PeopleIntent.Open -> emitEvent(PeopleEfect.NavigateToDetails(intent.personId))
         is PeopleIntent.Remove -> removeVisually(intent.person, intent.originalIndex)
         is PeopleIntent.Restore -> restoreVisually(intent.person, intent.originalIndex)
         PeopleIntent.Restored -> acknowledgeRestoredItem()
      }
   }

   private fun observePeople() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {
         _state.update { peopleUiState ->
            peopleUiState.copy(isLoading = true)
         }

         _repository.observeAll().collect { result ->
            result
               .onSuccess { databasePeople ->
                  val visiblePeople = _visualRemoval.visibleItems(databasePeople)
                  _state.update { peopleUiState ->
                     peopleUiState.copy(people = visiblePeople, isLoading = false)
                  }
               }
               .onFailure {
                  _state.update { peopleUiState ->
                     peopleUiState.copy(isLoading = false)
                  }
                  emitEvent(
                     PeopleEfect.ShowSnackbar(uiText(R.string.error_people_load))
                  )
               }
         }
      }
   }

   private fun removeVisually(person: Person, originalIndex: Int) {
      val result = _visualRemoval.remove(
         items = _state.value.people,
         item = person,
         originalIndex = originalIndex,
      )
      _state.update { peopleUiState ->
         peopleUiState.copy(people = result.items, restoredPersonId = null)
      }
      emitEvent(PeopleEfect.RequestRemove(person, result.originalIndex))
   }

   private fun restoreVisually(person: Person, originalIndex: Int) {
      val restoredPeople = _visualRemoval.restore(
         items = _state.value.people,
         item = person,
         originalIndex = originalIndex,
      )
      _state.update { peopleUiState ->
         peopleUiState.copy(people = restoredPeople, restoredPersonId = person.id)
      }
   }

   private fun acknowledgeRestoredItem() {
      _state.update { peopleUiState ->
         peopleUiState.copy(restoredPersonId = null)
      }
   }

   private fun emitEvent(event: PeopleEfect) {
      viewModelScope.launch {
         _events.send(event)
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}
