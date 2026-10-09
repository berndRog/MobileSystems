package de.rogallab.mobile.ui.tdrives.input_detail

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
import de.rogallab.mobile.shared.domain.utilities.newUuid
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

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
   private val _localTimeZone = TimeZone.currentSystemDefault()
   private val defaultStart = Clock.System.now()
      .toLocalDateTime(_localTimeZone)
      .let { now: LocalDateTime ->
         LocalDateTime(
            date = now.date,
            time = LocalTime(
               hour = now.hour,
               minute = now.minute,
            ),
         )
      }
      .toInstant(_localTimeZone)
   private var _isSaving = false
   private val _stateFlow = MutableStateFlow(
      if (_isNew) {
         val tDrive = TDrive(id = newUuid(), start = defaultStart)
         TDriveUiState(
            tDrive = tDrive,
            startInput = DateTimeText.format(tDrive.start.toLocalDateTime(_localTimeZone)),
            isNew = true,
         )
      } else TDriveUiState(isNew = false, isLoading = true)
   )
   val stateFlow: StateFlow<TDriveUiState> = _stateFlow.asStateFlow()
   private var _peopleJob: Job? = null
   private var _carsJob: Job? = null

   init {
      observePeople(); observeCars(); if (!_isNew) loadTDrive(_tDriveId!!)
   }

   fun onIntent(intent: TDriveIntent) {
      when (intent) {
         is TDriveIntent.PersonChanged -> update { it.copy(personId = intent.personId) }
         is TDriveIntent.CarChanged -> update { it.copy(carId = intent.carId) }
         is TDriveIntent.StartChanged -> _stateFlow.update { state: TDriveUiState -> state.copy(startInput = intent.value) }
         is TDriveIntent.CompletedChanged -> update { it.copy(isCompleted = intent.value) }
         TDriveIntent.RetryLoad -> if (!_isNew) { observePeople(); observeCars(); loadTDrive(_tDriveId!!) }
         TDriveIntent.Save -> save()
         TDriveIntent.Cancel -> navigateBack(BackReason.Cancel)
      }
   }

   private fun observePeople() {
      _peopleJob?.cancel()
      _stateFlow.update { state: TDriveUiState -> state.copy(peopleLoadError = null) }
      _peopleJob = viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result.onSuccess { items ->
               _stateFlow.update { state: TDriveUiState -> state.copy(people = items, peopleLoadError = null) }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_people_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDriveViewModel", error, throwable)
               _stateFlow.update { state: TDriveUiState -> state.copy(peopleLoadError = error) }
            }
         }
      }
   }
   private fun observeCars() {
      _carsJob?.cancel()
      _stateFlow.update { state: TDriveUiState -> state.copy(carsLoadError = null) }
      _carsJob = viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result.onSuccess { items ->
               _stateFlow.update { state: TDriveUiState -> state.copy(cars = items, carsLoadError = null) }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_cars_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDriveViewModel", error, throwable)
               _stateFlow.update { state: TDriveUiState -> state.copy(carsLoadError = error) }
            }
         }
      }
   }
   private fun loadTDrive(id: String) {
      _stateFlow.update { state: TDriveUiState ->
         state.copy(isLoading = true, driveLoadError = null, notFound = false)
      }
      viewModelScope.launch {
         _tDriveRepository.findById(id)
            .onSuccess { tDrive ->
               if (tDrive == null) {
                  val error = _stringProvider.getString(R.string.error_test_drive_not_found)
                  Alog.e("<-TDriveViewModel", error)
                  _stateFlow.update { state: TDriveUiState ->
                     state.copy(isLoading = false, driveLoadError = error, notFound = true)
                  }
               } else {
                  _stateFlow.update { state: TDriveUiState ->
                     state.copy(
                        tDrive = tDrive,
                        startInput = DateTimeText.format(tDrive.start.toLocalDateTime(_localTimeZone)),
                        isLoading = false, driveLoadError = null, notFound = false,
                     )
                  }
               }
            }.onFailure { throwable ->
               val fallback = _stringProvider.getString(R.string.error_test_drive_load)
               val error = throwable.userMessageOr(fallback)
               Alog.e("<-TDriveViewModel", error, throwable)
               _stateFlow.update { state: TDriveUiState ->
                  state.copy(isLoading = false, driveLoadError = error, notFound = false)
               }
            }
      }
   }
   private fun update(transform: (TDrive) -> TDrive) {
      _stateFlow.update { state: TDriveUiState -> state.tDrive?.let { state.copy(tDrive = transform(it)) } ?: state }
   }
   private fun save() {
      if (_isSaving || _stateFlow.value.isLoading || _stateFlow.value.loadError != null) return
      val state = _stateFlow.value
      val tDrive = state.tDrive ?: return
      val start = _validator.parseStart(state.startInput)
      if (start == null) {
         showError(_validator.validateStart(state.startInput).orEmpty()); return
      }
      val normalized = tDrive.copy(start = start.toInstant(_localTimeZone))
      val error = _validator.validateTestDrive(normalized, state.startInput)
      if (error != null) { showError(error); return }
      _stateFlow.update { current: TDriveUiState -> current.copy(tDrive = normalized) }
      _isSaving = true
      viewModelScope.launch {
         val result = if (_isNew) _tDriveRepository.create(normalized) else _tDriveRepository.update(normalized)
         result.onSuccess {
            _effectDelegate.emit(TDriveEffect.ShowMessage(_stringProvider.getString(R.string.message_test_drive_saved)))
            _effectDelegate.emit(TDriveEffect.NavigateBack(BackReason.Save))
         }.onFailure { throwable ->
            val fallback = _stringProvider.getString(R.string.error_test_drive_save)
            _effectDelegate.emit(
               TDriveEffect.ShowError(throwable.userMessageOr(fallback))
            )
         }
         _isSaving = false
      }
   }
   private fun navigateBack(reason: BackReason) { viewModelScope.launch { _effectDelegate.emit(TDriveEffect.NavigateBack(reason)) } }
   private fun showError(message: String) { viewModelScope.launch { showErrorNow(message) } }
   private suspend fun showErrorNow(message: String) { _effectDelegate.emit(TDriveEffect.ShowError(message)) }
}
