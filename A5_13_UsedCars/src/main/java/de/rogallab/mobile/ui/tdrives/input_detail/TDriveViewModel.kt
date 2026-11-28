package de.rogallab.mobile.ui.tdrives.input_detail

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.shared.data.network.userMessageOr
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.newUuid
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlin.time.Clock
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
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<TDriveEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<TDriveEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { TDriveEffect.ShowError(it) },
      _messageEffect = { TDriveEffect.ShowMessage(it) },
      _tag = "<-TDriveViewModel",
   )

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
   private var _isClosing = false
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

   init {
      observePeople(); observeCars(); if (!_isNew) loadTDrive(_tDriveId!!)
   }

   fun onIntent(intent: TDriveIntent) {
      if ((_isSaving || _isClosing) &&
         (intent is TDriveIntent.PersonChanged ||
            intent is TDriveIntent.CarChanged ||
            intent is TDriveIntent.StartChanged ||
            intent is TDriveIntent.CompletedChanged)) return

      when (intent) {
         is TDriveIntent.PersonChanged -> changePerson(intent.personId)
         is TDriveIntent.CarChanged -> changeCar(intent.carId)
         is TDriveIntent.StartChanged -> changeStart(intent.value)
         is TDriveIntent.CompletedChanged -> changeCompleted(intent.value)
         TDriveIntent.Save -> save()
         TDriveIntent.Cancel -> navigateBack(BackReason.Cancel)
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         delay(_timing.delayRead)

         _personRepository.observeAll().collect { result ->
            result.onSuccess { people -> _stateFlow.update { state: TDriveUiState -> state.copy(people = people) } }
               .onFailure { throwable ->
                  val fallback = _stringProvider.getString(R.string.error_people_load)
                  _effects.showError(throwable.userMessageOr(fallback), throwable)
               }
         }
      }
   }
   private fun observeCars() {
      viewModelScope.launch {
         delay(_timing.delayRead)

         _carRepository.observeAll().collect { result ->
            result.onSuccess { cars -> _stateFlow.update { state: TDriveUiState -> state.copy(cars = cars) } }
               .onFailure { throwable ->
                  val fallback = _stringProvider.getString(R.string.error_cars_load)
                  _effects.showError(throwable.userMessageOr(fallback), throwable)
               }
         }
      }
   }
   private fun loadTDrive(id: String) {
      viewModelScope.launch {
         delay(_timing.delayRead)

         _tDriveRepository.findById(id).onSuccess { tDrive ->
            if (tDrive == null) {
               _stateFlow.update { state: TDriveUiState -> state.copy(isLoading = false) }
               _effects.showError(R.string.error_test_drive_not_found)
            } else {
               _stateFlow.update { state: TDriveUiState ->
                  state.copy(
                     tDrive = tDrive,
                     startInput = DateTimeText.format(tDrive.start.toLocalDateTime(_localTimeZone)),
                     isLoading = false,
                  )
               }
            }
         }.onFailure { throwable ->
            _stateFlow.update { state: TDriveUiState -> state.copy(isLoading = false) }
            val fallback = _stringProvider.getString(R.string.error_test_drive_load)
            _effects.showError(throwable.userMessageOr(fallback), throwable)
         }
      }
   }
   private fun changePerson(personId: String?) =
      _stateFlow.update { state ->
         val tDrive = state.tDrive ?: return@update state
         state.copy(tDrive = tDrive.copy(personId = personId))
      }

   private fun changeCar(carId: String?) =
      _stateFlow.update { state ->
         val tDrive = state.tDrive ?: return@update state
         state.copy(tDrive = tDrive.copy(carId = carId))
      }

   private fun changeStart(startInput: String) =
      _stateFlow.update { state ->
         state.copy(startInput = startInput)
      }

   private fun changeCompleted(isCompleted: Boolean) =
      _stateFlow.update { state ->
         val tDrive = state.tDrive ?: return@update state
         state.copy(tDrive = tDrive.copy(isCompleted = isCompleted))
      }
   private fun save() {
      if (_isSaving) return
      val state = _stateFlow.value
      val tDrive = state.tDrive ?: return
      val start = _validator.parseStart(state.startInput)
      if (start == null) {
         _effects.launchShowError(_validator.validateStart(state.startInput).orEmpty()); return
      }
      val normalized = tDrive.copy(start = start.toInstant(_localTimeZone))
      val error = _validator.validateTestDrive(normalized, state.startInput)
      if (error != null) { _effects.launchShowError(error); return }
      _stateFlow.update { current: TDriveUiState -> current.copy(tDrive = normalized) }
      _isSaving = true
      val job = viewModelScope.launch {
         delay(_timing.delayWrite)

         val result = if (_isNew) _tDriveRepository.create(normalized) else _tDriveRepository.update(normalized)
         result.onSuccess {
            _isClosing = true
            _effects.showMessage(R.string.message_test_drive_saved)
            _effects.emit(TDriveEffect.NavigateBack(BackReason.Save))
         }.onFailure { throwable ->
            val fallback = _stringProvider.getString(R.string.error_test_drive_save)
            _effects.showError(throwable.userMessageOr(fallback), throwable)
         }

      }
      // Reset the existing save guard even when effect delivery is cancelled.
      job.invokeOnCompletion {
         _isSaving = false
      }
   }




   private fun navigateBack(reason: BackReason) {
      _isClosing = true
      _effects.launchEmit(TDriveEffect.NavigateBack(reason))
   }

}

/*
 * Didaktik und Lernziele
 *
 * - EffectDispatcher bereitet Meldungen auf und sendet über denselben Delegate.
 *   Persistente Fehler und fachliche Entscheidungen bleiben im ViewModel.
 * - launch-Aufrufe starten Versand aus Intents; showError, showMessage und emit
 *   bleiben im Kontext laufender Operationen und erhalten deren Reihenfolge.
 * - Result-Callbacks, try/catch/finally, Guards und technische Cleanup-Abläufe
 *   bleiben erhalten. Ein Fehler wird am Aufrufer nicht zusätzlich protokolliert.
 *
 * Lernziele:
 * - Meldungsaufbereitung von fachlicher Fehlerbehandlung unterscheiden.
 * - Coroutine-Lebensdauer und sequenziellen Versand typisierter Effects erhalten.
 */
