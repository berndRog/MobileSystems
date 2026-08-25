package de.rogallab.mobile.ui.count

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CountViewModel: ViewModel() {

   // Define a MutableStateFlow with the initial CountUiState
   private val _stateFlow: MutableStateFlow<Int> = MutableStateFlow(0)  // factory method

   // Expose MutableStateFlow as a read-only StateFlow to the UI
   val stateFlow: StateFlow<Int> = _stateFlow.asStateFlow()

   // Increment the count value and update the MutableStateFlow
   // with a new CountUiState
   fun incrementCount() {
      _stateFlow.update { count: Int ->
         count + 1
      }
      Alog.d(TAG, "incrementCount() count=${_stateFlow.value}")
   }

   override fun onCleared() {
      super.onCleared()
   }

   companion object {
      private const val TAG = "<-CountViewModel"
   }

}