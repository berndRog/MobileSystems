package de.rogallab.mobile.ui.tdrives.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.ITDriveReadModel
import de.rogallab.mobile.domain.readmodels.TDriveListItem
import de.rogallab.mobile.domain.usecases.TDriveUcDelete
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

// Reads through ITDriveReadModel and deletes through TDriveUcDelete.
// The list ViewModel needs no repository.
class TDrivesViewModel(
   private val _readModel: ITDriveReadModel,
   private val _tDriveUcDelete: TDriveUcDelete,
   private val _stringProvider: IStringProvider,
   private val _timing: AppTiming = AppTiming(),
   private val _effectDelegate: EffectDelegate<TDrivesEffect>,
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

   private var _observeJob: Job? = null

   init {
      observeTDrives()
   }

   fun onIntent(intent: TDrivesIntent) {
      when (intent) {
         TDrivesIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observeTDrives()
            }
         TDrivesIntent.Create -> _effects.launchEmit(TDrivesEffect.NavigateTo(null))
         is TDrivesIntent.Detail -> _effects.launchEmit(TDrivesEffect.NavigateTo(intent.tDriveId))
         is TDrivesIntent.RequestRemove -> requestRemove(intent.tDriveId)
         is TDrivesIntent.ConfirmRemove -> confirmRemove(intent.tDriveId)
      }
   }

   // The read model flow emits again whenever TDrive, Person or Car changes,
   // so driver and car names stay current.
   private fun observeTDrives() {
      val previousJob = _observeJob
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      _observeJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _readModel.observeListItems().collect { result: Result<List<TDriveListItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(tDrives = items, isLoading = false, loadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isLoading = false,
                           loadFailure = _stringProvider.getString(R.string.error_test_drives_load))
                     }
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false,
                  loadFailure = _stringProvider.getString(R.string.error_test_drives_load))
            }
         }
      }
   }


   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(tDriveId: String) {
      val item = findItem(tDriveId) ?: return
      viewModelScope.launch {
         _effects.emit(TDrivesEffect.ConfirmRemove(
               message = _stringProvider.getString(R.string.message_test_drive_remove_confirm),
               actionLabel = _stringProvider.getString(R.string.action_confirm),
               tDriveId = item.id,
            ))
      }
   }

   // The confirmed action passes only the id; the use case loads the aggregate.
   private fun confirmRemove(tDriveId: String) {
      val item = findItem(tDriveId) ?: return
      viewModelScope.launch {
         delay(_timing.delayWrite)
         _tDriveUcDelete(item.id)
            .onFailure { throwable ->
               _effects.showError(R.string.error_test_drive_delete, throwable)
            }
         // The read model flow updates the list after successful deletion.
      }
   }

   private fun findItem(tDriveId: String): TDriveListItem? {
      val item = _stateFlow.value.tDrives.find { it.id == tDriveId }
      if (item == null) _effects.launchShowError(R.string.error_test_drive_not_found)
      return item
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
