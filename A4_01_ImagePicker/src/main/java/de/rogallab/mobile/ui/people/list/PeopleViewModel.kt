package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())
   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   private var _observeJob: Job? = null

   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.RequestRemove -> requestRemove(intent.personId)
         is PeopleIntent.ConfirmRemove -> confirmRemove(intent.personId)
      }
   }

   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(PeopleEffect.NavigateTo(personId))
      }
   }

   private fun requestRemove(personId: String) {
      val person = _stateFlow.value.people.find { person: Person ->
         person.id == personId
      }
      if (person == null) {
         emitPersonNotFound()
         return
      }

      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_person_remove_confirm,
            person.firstName,
            person.lastName,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)

         _effectDelegate.emit(
            PeopleEffect.ConfirmRemove(
               message = message,
               actionLabel = actionLabel,
               personId = person.id,
            )
         )
      }
   }

   private fun confirmRemove(personId: String) {
      val person = _stateFlow.value.people.find { person: Person ->
         person.id == personId
      }
      if (person == null) {
         emitPersonNotFound()
         return
      }
      remove(person)
   }

   private fun observePeople() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }
         delay(1000)

         _repository.observeAll().collect { result: Result<List<Person>> ->
            result
               .onSuccess { people ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(people = people, isLoading = false)
                  }
               }
               .onFailure {
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }
                  val error =
                     _stringProvider.getString(R.string.error_people_observe)
                  _effectDelegate.emit(PeopleEffect.ShowError(error))
               }
         }
      }
   }

   private fun remove(person: Person) {
      viewModelScope.launch {
         _repository.remove(person)
            .onFailure {
               val error =
                  _stringProvider.getString(R.string.error_person_remove)
               _effectDelegate.emit(PeopleEffect.ShowError(error))
            }
      }
   }

   private fun emitPersonNotFound() {
      viewModelScope.launch {
         val error =
            _stringProvider.getString(R.string.error_person_not_found)
         _effectDelegate.emit(PeopleEffect.ShowError(error))
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}
