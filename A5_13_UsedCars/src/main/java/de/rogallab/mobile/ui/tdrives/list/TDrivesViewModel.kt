package de.rogallab.mobile.ui.tdrives.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.data.network.userMessageOr
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import kotlinx.coroutines.Job
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
   private var _drivesJob: Job? = null
   private var _peopleJob: Job? = null
   private var _carsJob: Job? = null

   init {
      observeTDrives(); observePeople(); observeCars()
   }

   fun onIntent(intent: TDrivesIntent) {
      when (intent) {
         TDrivesIntent.RetryLoad -> { observeTDrives(); observePeople(); observeCars() }
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
            .onFailure { throwable ->
               emitErrorNow(R.string.error_test_drive_delete, throwable)
            }
      }
   }

   private fun observeTDrives() {
      _drivesJob?.cancel()
      _stateFlow.update { state: TDrivesUiState -> state.copy(isLoading = true, drivesLoadError = null) }
      _drivesJob = viewModelScope.launch {
         _tDriveRepository.observeAll().collect { result: Result<List<TDrive>> ->
            result.onSuccess { drives ->
               _stateFlow.update { state: TDrivesUiState ->
                  state.copy(tDrives = drives, isLoading = false, drivesLoadError = null)
               }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_test_drives_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDrivesViewModel", error, throwable)
               _stateFlow.update { state: TDrivesUiState ->
                  state.copy(isLoading = false, drivesLoadError = error)
               }
            }
         }
      }
   }

   private fun observePeople() {
      _peopleJob?.cancel()
      _stateFlow.update { state: TDrivesUiState -> state.copy(peopleLoadError = null) }
      _peopleJob = viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { items ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(people = items, peopleLoadError = null) }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_people_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDrivesViewModel", error, throwable)
               _stateFlow.update { state: TDrivesUiState -> state.copy(peopleLoadError = error) }
            }
         }
      }
   }

   private fun observeCars() {
      _carsJob?.cancel()
      _stateFlow.update { state: TDrivesUiState -> state.copy(carsLoadError = null) }
      _carsJob = viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result.onSuccess { items ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(cars = items, carsLoadError = null) }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_cars_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDrivesViewModel", error, throwable)
               _stateFlow.update { state: TDrivesUiState -> state.copy(carsLoadError = error) }
            }
         }
      }
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }
   private suspend fun emitErrorNow(
      resourceId: Int,
      throwable: Throwable? = null,
   ) {
      val fallback = _stringProvider.getString(resourceId)
      val message = throwable?.userMessageOr(fallback) ?: fallback
      _effectDelegate.emit(TDrivesEffect.ShowError(message))
   }
}
