package de.rogallab.mobile.ui.people.create_detail.mvvm

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// MVVM ViewModel for PersonScreen.

class PersonViewModelMvvm(
   // repository: IPersonRepository
) : ViewModel() {

   // StateFlow
   private val _stateFlow: MutableStateFlow<PersonUiState> = MutableStateFlow(PersonUiState())
   val stateFlow: StateFlow<PersonUiState> = _stateFlow.asStateFlow()

   // Processing events
   fun changeFirstName(firstName: String) {
      Alog.d(TAG, "changeFirstName: ${firstName.trim()}")
      _stateFlow.update { state: PersonUiState ->

         val person = state.person
         state.copy(person = person.copy(firstName = firstName.trim()))
//       state.copy(person = state.person.copy(firstName = firstName.trim()))
      }
   }

   fun changeLastName(lastName: String) {
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
                              //12345678901234567890
      const val TAG: String = "<-PersonViewModelMvvm"
   }

}