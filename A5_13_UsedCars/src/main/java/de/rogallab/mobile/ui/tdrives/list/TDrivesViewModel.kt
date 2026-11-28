package de.rogallab.mobile.ui.tdrives.list

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
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
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
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<TDrivesEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<TDrivesEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { TDrivesEffect.ShowError(it) },
      _messageEffect = { TDrivesEffect.ShowMessage(it) },
      _tag = "<-TDrivesViewModel",
   )

   private val _stateFlow = MutableStateFlow(TDrivesUiState())
   val stateFlow: StateFlow<TDrivesUiState> = _stateFlow.asStateFlow()

   init {
      observeTDrives(); observePeople(); observeCars()
   }

   fun onIntent(intent: TDrivesIntent) {
      when (intent) {
         TDrivesIntent.Create -> _effects.launchEmit(TDrivesEffect.NavigateTo(null))
         is TDrivesIntent.Detail -> _effects.launchEmit(TDrivesEffect.NavigateTo(intent.tDriveId))
         is TDrivesIntent.RequestRemove -> requestRemove(intent.tDriveId)
         is TDrivesIntent.ConfirmRemove -> confirmRemove(intent.tDriveId)
      }
   }


   private fun requestRemove(tDriveId: String) {
      if (_stateFlow.value.tDrives.none { it.id == tDriveId }) {
         _effects.launchShowError(R.string.error_test_drive_not_found); return
      }
      viewModelScope.launch {
         _effects.emit(TDrivesEffect.ConfirmRemove(
            message = _stringProvider.getString(R.string.message_test_drive_remove_confirm),
            actionLabel = _stringProvider.getString(R.string.action_confirm),
            tDriveId = tDriveId,
         ))
      }
   }

   private fun confirmRemove(tDriveId: String) {
      val tDrive = _stateFlow.value.tDrives.find { it.id == tDriveId }
      if (tDrive == null) {
         _effects.launchShowError(R.string.error_test_drive_not_found); return
      }
      viewModelScope.launch {
         delay(_timing.delayWrite)

         _tDriveRepository.remove(tDrive)
            .onFailure { throwable ->
               _effects.showError(throwable.userMessageOr(_stringProvider.getString(R.string.error_test_drive_delete)), throwable)
            }
      }
   }

   private fun observeTDrives() {
      viewModelScope.launch {
         _stateFlow.update { state: TDrivesUiState -> state.copy(isLoading = true) }
         delay(_timing.delayRead)

         _tDriveRepository.observeAll().collect { result: Result<List<TDrive>> ->
            result.onSuccess { drives ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(tDrives = drives, isLoading = false) }
            }.onFailure { throwable ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(isLoading = false) }
               _effects.showError(throwable.userMessageOr(_stringProvider.getString(R.string.error_test_drives_load)), throwable)
            }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         delay(_timing.delayRead)

         _personRepository.observeAll().collect { result ->
            result.onSuccess { people ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(people = people) }
            }.onFailure { throwable ->
               _effects.showError(throwable.userMessageOr(_stringProvider.getString(R.string.error_people_load)), throwable)
            }
         }
      }
   }

   private fun observeCars() {
      viewModelScope.launch {
         delay(_timing.delayRead)

         _carRepository.observeAll().collect { result ->
            result.onSuccess { cars ->
               _stateFlow.update { state: TDrivesUiState -> state.copy(cars = cars) }
            }.onFailure { throwable ->
               _effects.showError(throwable.userMessageOr(_stringProvider.getString(R.string.error_cars_load)), throwable)
            }
         }
      }
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
