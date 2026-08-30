package de.rogallab.mobile.ui.tdrives.input_detail

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
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.people.create_detail.BackReason
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TDriveViewModel(
   val tDriveId: String?,
   private val _tDriveRepository: ITDriveRepository,
   private val _personRepository: IPersonRepository,
   private val _carRepository: ICarRepository,
   private val _stringProvider: IStringProvider,
   private val _validator: TDriveValidator,
   private val _effectDelegate: EffectDelegate<TDriveEffect>,
) : ViewModel(), IEffectSource<TDriveEffect> by _effectDelegate {

   private val _tDriveId = tDriveId?.takeUnless(String::isBlank)
   private val _isNew = _tDriveId == null
   private val defaultStart = LocalDateTime.now().withSecond(0).withNano(0)
   private var _isSaving = false
   private val _stateFlow = MutableStateFlow(
      if (_isNew) {
         val tDrive = TDrive(id = UUID.randomUUID().toString(), start = defaultStart)
         TDriveUiState(tDrive = tDrive, startInput = DateTimeText.format(tDrive.start), isNew = true)
      } else TDriveUiState(isNew = false, isLoading = true)
   )
   val stateFlow: StateFlow<TDriveUiState> = _stateFlow.asStateFlow()

   init {
      observePeople(); observeCars(); if (!_isNew) loadTDrive(_tDriveId!!)
   }

   fun onIntent(intent: TDriveIntent) {
      when (intent) {
         is TDriveIntent.PersonChanged -> update { it.copy(personId = intent.personId) }
         is TDriveIntent.CarChanged -> update { it.copy(carId = intent.carId) }
         is TDriveIntent.StartChanged -> _stateFlow.update { state: TDriveUiState -> state.copy(startInput = intent.value) }
         is TDriveIntent.NotesChanged -> update { it.copy(notes = intent.value.trim().takeUnless(String::isBlank)) }
         is TDriveIntent.CompletedChanged -> update { it.copy(isCompleted = intent.value) }
         TDriveIntent.Save -> save()
         TDriveIntent.Cancel -> navigateBack(BackReason.Cancel)
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { people -> _stateFlow.update { state: TDriveUiState -> state.copy(people = people) } }
               .onFailure { showErrorNow(_stringProvider.getString(R.string.error_people_load)) }
         }
      }
   }
   private fun observeCars() {
      viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result.onSuccess { cars -> _stateFlow.update { state: TDriveUiState -> state.copy(cars = cars) } }
               .onFailure { showErrorNow(_stringProvider.getString(R.string.error_cars_load)) }
         }
      }
   }
   private fun loadTDrive(id: String) {
      viewModelScope.launch {
         _tDriveRepository.findById(id).onSuccess { tDrive ->
            if (tDrive == null) {
               _stateFlow.update { state: TDriveUiState -> state.copy(isLoading = false) }
               showErrorNow(_stringProvider.getString(R.string.error_test_drive_not_found))
            } else {
               _stateFlow.update { state: TDriveUiState ->
                  state.copy(tDrive = tDrive, startInput = DateTimeText.format(tDrive.start), isLoading = false)
               }
            }
         }.onFailure {
            _stateFlow.update { state: TDriveUiState -> state.copy(isLoading = false) }
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_load))
         }
      }
   }
   private fun update(transform: (TDrive) -> TDrive) {
      _stateFlow.update { state: TDriveUiState -> state.tDrive?.let { state.copy(tDrive = transform(it)) } ?: state }
   }
   private fun save() {
      if (_isSaving) return
      val state = _stateFlow.value
      val tDrive = state.tDrive ?: return
      val start = _validator.parseStart(state.startInput)
      if (start == null) {
         showError(_validator.validateStart(state.startInput).orEmpty()); return
      }
      val normalized = tDrive.copy(start = start, notes = tDrive.notes?.trim()?.takeUnless(String::isBlank))
      val error = _validator.validateTestDrive(normalized, state.startInput)
      if (error != null) { showError(error); return }
      _stateFlow.update { current: TDriveUiState -> current.copy(tDrive = normalized) }
      _isSaving = true
      viewModelScope.launch {
         val result = if (_isNew) _tDriveRepository.create(normalized) else _tDriveRepository.update(normalized)
         result.onSuccess {
            _effectDelegate.emit(TDriveEffect.ShowMessage(_stringProvider.getString(R.string.message_test_drive_saved)))
            _effectDelegate.emit(TDriveEffect.NavigateBack(BackReason.Save))
         }.onFailure {
            _effectDelegate.emit(TDriveEffect.ShowError(_stringProvider.getString(R.string.error_test_drive_save)))
         }
         _isSaving = false
      }
   }
   private fun navigateBack(reason: BackReason) { viewModelScope.launch { _effectDelegate.emit(TDriveEffect.NavigateBack(reason)) } }
   private fun showError(message: String) { viewModelScope.launch { showErrorNow(message) } }
   private suspend fun showErrorNow(message: String) { _effectDelegate.emit(TDriveEffect.ShowError(message)) }
}
