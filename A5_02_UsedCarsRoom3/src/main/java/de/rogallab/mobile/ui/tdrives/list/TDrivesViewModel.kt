package de.rogallab.mobile.ui.tdrives.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.ui.common.VisualRemovalController
import de.rogallab.mobile.ui.common.uiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TDrivesViewModel(
   private val _tDriveRepository: ITDriveRepository,
   private val _personRepository: IPersonRepository,
   private val _carRepository: ICarRepository,
) : ViewModel() {

   private val _state = MutableStateFlow(TDrivesUiState())
   val state: StateFlow<TDrivesUiState> = _state.asStateFlow()

   private val _events = Channel<TDrivesEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   // delegates the temporary visual removal of list items until the repository confirms
   // the deletion or the user restores the item with Undo
   private val _visualRemoval = VisualRemovalController<TDrive>(TDrive::id)

   init {
      observeTDrives()
      observePeople()
      observeCars()
   }

   fun onIntent(intent: TDrivesIntent) {
      when (intent) {
         TDrivesIntent.Create -> emitEvent(TDrivesEvent.NavigateToCreate)
         is TDrivesIntent.Open -> emitEvent(TDrivesEvent.NavigateToDetails(intent.testDriveId))
         is TDrivesIntent.Remove -> removeVisually(intent.tDrive, intent.originalIndex)
         is TDrivesIntent.Restore -> restoreVisually(intent.tDrive, intent.originalIndex)
         TDrivesIntent.Restored -> _state.update { currentState ->
            currentState.copy(restoredTDriveId = null)
         }
      }
   }

   private fun observeTDrives() {
      viewModelScope.launch {
         _state.update { currentState -> currentState.copy(isLoading = true) }
         _tDriveRepository.observeAll().collect { result ->
            result
               .onSuccess { databaseTDrives ->
                  val visibleTDrives = _visualRemoval.visibleItems(databaseTDrives)
                  _state.update { currentState ->
                     currentState.copy(tDrives = visibleTDrives, isLoading = false)
                  }
               }
               .onFailure {
                  _state.update { currentState -> currentState.copy(isLoading = false) }
                  emitEvent(
                     TDrivesEvent.ShowSnackbar(
                        uiText(R.string.error_test_drives_load)
                     )
                  )
               }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result
               .onSuccess { people ->
                  _state.update { currentState ->
                     currentState.copy(people = people)
                  }
               }
               .onFailure {
                  emitEvent(
                     TDrivesEvent.ShowSnackbar(uiText(R.string.error_people_load))
                  )
               }
         }
      }
   }

   private fun observeCars() {
      viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result
               .onSuccess { cars ->
                  _state.update { currentState ->
                     currentState.copy(cars = cars)
                  }
               }
               .onFailure {
                  emitEvent(
                     TDrivesEvent.ShowSnackbar(uiText(R.string.error_cars_load))
                  )
               }
         }
      }
   }

   private fun removeVisually(tDrive: TDrive, originalIndex: Int) {
      val result = _visualRemoval.remove(
         items = _state.value.tDrives,
         item = tDrive,
         originalIndex = originalIndex,
      )
      _state.update { currentState ->
         currentState.copy(tDrives = result.items, restoredTDriveId = null)
      }
      emitEvent(TDrivesEvent.RequestRemove(tDrive, result.originalIndex))
   }

   private fun restoreVisually(tDrive: TDrive, originalIndex: Int) {
      val restoredTDrives = _visualRemoval.restore(
         items = _state.value.tDrives,
         item = tDrive,
         originalIndex = originalIndex,
      )
      _state.update { currentState ->
         currentState.copy(tDrives = restoredTDrives, restoredTDriveId = tDrive.id)
      }
   }

   private fun emitEvent(event: TDrivesEvent) {
      viewModelScope.launch { _events.send(event) }
   }

   companion object {
      private const val TAG = "<-TDrivesViewModel"
   }
}
