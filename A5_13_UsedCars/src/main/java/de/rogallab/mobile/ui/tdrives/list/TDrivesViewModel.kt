package de.rogallab.mobile.ui.tdrives.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TDrivesViewModel(
   private val _tDriveRepository: ITDriveRepository,
   private val _personRepository: IPersonRepository,
   private val _carRepository: ICarRepository,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<TDrivesEffect>,
) : ViewModel(), IEffectSource<TDrivesEffect> by _effectDelegate {

   private val _stateFlow = MutableStateFlow(TDrivesUiState())
   val stateFlow: StateFlow<TDrivesUiState> = _stateFlow.asStateFlow()

   init {
      observeTDrives(); observePeople(); observeCars()
   }

   fun onIntent(intent: TDrivesIntent) {
      when (intent) {
         TDrivesIntent.Create -> navigateTo(null)
         is TDrivesIntent.Detail -> navigateTo(intent.tDriveId)
         is TDrivesIntent.RequestRemove -> requestRemove(intent.tDriveId)
         is TDrivesIntent.ConfirmRemove -> confirmRemove(intent.tDriveId)
      }
   }

   private fun navigateTo(tDriveId: String?) {
      viewModelScope.launch { _effectDelegate.emit(TDrivesEffect.NavigateTo(tDriveId)) }
   }

   private fun requestRemove(tDriveId: String) {
      if (_stateFlow.value.tDrives.none { it.id == tDriveId }) {
         emitError(R.string.error_test_drive_not_found); return
      }
      viewModelScope.launch {
         _effectDelegate.emit(TDrivesEffect.ConfirmRemove(
            message = _stringProvider.getString(R.string.message_test_drive_remove_confirm),
            actionLabel = _stringProvider.getString(R.string.action_confirm),
            tDriveId = tDriveId,
         ))
      }
   }

   private fun confirmRemove(tDriveId: String) {
      val tDrive = _stateFlow.value.tDrives.find { it.id == tDriveId }
      if (tDrive == null) {
         emitError(R.string.error_test_drive_not_found); return
      }
      viewModelScope.launch {
         _tDriveRepository.remove(tDrive)
            .onFailure { emitErrorNow(R.string.error_test_drive_delete) }
      }
   }

   private fun observeTDrives() {
      viewModelScope.launch {
         _stateFlow.update { state: TDrivesUiState -> state.copy(isLoading = true) }
         _tDriveRepository.observeAll().collect { result: Result<List<TDrive>> ->
            result.onSuccess { drives ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(tDrives = drives, isLoading = false) }
            }.onFailure {
               _stateFlow.update { state: TDrivesUiState -> state.copy(isLoading = false) }
               emitErrorNow(R.string.error_test_drives_load)
            }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { people ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(people = people) }
            }.onFailure { emitErrorNow(R.string.error_people_load) }
         }
      }
   }

   private fun observeCars() {
      viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result.onSuccess { cars ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(cars = cars) }
            }.onFailure { emitErrorNow(R.string.error_cars_load) }
         }
      }
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }
   private suspend fun emitErrorNow(resourceId: Int) {
      _effectDelegate.emit(TDrivesEffect.ShowError(_stringProvider.getString(resourceId)))
   }
}
