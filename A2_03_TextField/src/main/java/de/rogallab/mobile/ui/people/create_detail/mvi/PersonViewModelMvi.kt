package de.rogallab.mobile.ui.people.create_detail.mvi

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PersonViewModelMvi(
   // repository: IPersonRepository
) : ViewModel() {

   val _initialState = PersonUiState(
      person = Person(),
      isLoading = false
   )

   // StateFlow
   private val _stateFlow: MutableStateFlow<PersonUiState> = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<PersonUiState> = _stateFlow.asStateFlow()

   // Processing intents
   fun onIntent(intent: PersonIntent) {
      Alog.d(TAG, "intent: $intent")
      when(intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   // Actions are private!!!
   private fun changeFirstName(firstName: String) {
      Alog.d(TAG, "changeFirstName: ${firstName}")
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = firstName.trim()))
      }
   }
   private fun changeLastName(lastName: String) {
      Alog.d(TAG, "changeLastName: $lastName")
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(lastName = lastName.trim()))
      }
   }

   fun save() {
      Alog.d(TAG, "save: ${_stateFlow.value.person}")
   }

   fun cancel() {
      Alog.d(TAG, "cancel()")
      _stateFlow.update { state: PersonUiState ->
         state.copy(person = state.person.copy(firstName = "", lastName = ""))
      }
   }

   companion object {
      const val TAG: String = "<-PeopleViewModelMvi"
   }

}