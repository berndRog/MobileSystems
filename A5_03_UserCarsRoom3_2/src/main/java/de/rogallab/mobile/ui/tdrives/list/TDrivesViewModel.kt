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
         TDrivesIntent.Create -> navigateTo(null)
         is TDrivesIntent.Detail -> navigateTo(intent.tDriveId)
         is TDrivesIntent.RequestRemove -> requestRemove(intent.tDriveId)
         is TDrivesIntent.ConfirmRemove -> confirmRemove(intent.tDriveId)
      }
   }

   private fun navigateTo(tDriveId: String?) {
      viewModelScope.launch { _effectDelegate.emit(TDrivesEffect.NavigateTo(tDriveId)) }
   }

   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(tDriveId: String) {
      val item = findItem(tDriveId) ?: return
      viewModelScope.launch {
         _effectDelegate.emit(
            TDrivesEffect.ConfirmRemove(
               message = _stringProvider.getString(R.string.message_test_drive_remove_confirm),
               actionLabel = _stringProvider.getString(R.string.action_confirm),
               tDriveId = item.id,
            )
         )
      }
   }

   // The confirmed action passes only the id; the use case loads the aggregate.
   private fun confirmRemove(tDriveId: String) {
      val item = findItem(tDriveId) ?: return
      viewModelScope.launch {
         try {
            delay(_timing.delayWrite)
            _tDriveUcDelete(item.id).getOrThrow()
            // On success the read model flow emits the list without this test drive.
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            emitErrorNow(R.string.error_test_drive_delete)
         }
      }
   }

   // The read model flow emits again whenever TDrive, Person or Car changes,
   // so driver and car names stay current.
   private fun observeTDrives() {
      val previousJob = _observeJob
      previousJob?.cancel()
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
                     currentCoroutineContext().ensureActive()
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

   private fun findItem(tDriveId: String): TDriveListItem? {
      val item = _stateFlow.value.tDrives.find { it.id == tDriveId }
      if (item == null) emitError(R.string.error_test_drive_not_found)
      return item
   }

   private fun emitError(resourceId: Int) {
      viewModelScope.launch { emitErrorNow(resourceId) }
   }

   private suspend fun emitErrorNow(resourceId: Int) {
      _effectDelegate.emit(TDrivesEffect.ShowError(_stringProvider.getString(resourceId)))
   }
}
