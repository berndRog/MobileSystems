package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.usecases.CarUcDelete
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class CarsViewModel(
   private val _readModel: ICarReadModel,
   private val _carUcDelete: CarUcDelete,
   private val _stringProvider: IStringProvider,
   private val _effectDelegate: EffectDelegate<CarsEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<CarsEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<CarsEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { CarsEffect.ShowError(it) },
      _messageEffect = { CarsEffect.ShowMessage(it) },
      _tag = TAG,
   )

   private val _stateFlow = MutableStateFlow(CarsUiState())
   val stateFlow: StateFlow<CarsUiState> = _stateFlow.asStateFlow()
   private var _observeJob: Job? = null

   init {
      observeCars()
   }

   fun onIntent(intent: CarsIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         CarsIntent.RetryLoad ->
            if (!_stateFlow.value.isLoading && _stateFlow.value.loadFailure != null) {
               observeCars()
            }
         CarsIntent.Create -> _effects.launchEmit(CarsEffect.NavigateTo(null))
         is CarsIntent.Detail -> _effects.launchEmit(CarsEffect.NavigateTo(intent.carId))
         is CarsIntent.RequestRemove -> requestRemove(intent.carId)
         is CarsIntent.ConfirmRemove -> confirmRemove(intent.carId)
      }
   }

   // The read model flow emits again whenever Car or Person changes,
   // so seller names stay current without observing people separately.
   private fun observeCars() {
      val previousJob = _observeJob
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      _observeJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _readModel.observeListItems().collect { result: Result<List<CarListItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(cars = items, isLoading = false, loadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isLoading = false,
                           loadFailure = _stringProvider.getString(R.string.error_cars_load))
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
                  loadFailure = _stringProvider.getString(R.string.error_cars_load))
            }
         }
      }
   }


   // A swipe asks for confirmation before anything is deleted.
   private fun requestRemove(carId: String) {
      val item = findItem(carId) ?: return
      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_car_remove_confirm,
            item.displayName,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)
         _effects.emit(CarsEffect.ConfirmRemove(message, actionLabel, item.id))
      }
   }

   // The confirmed action passes only the id; the use case loads the aggregate.
   private fun confirmRemove(carId: String) {
      val item = findItem(carId) ?: return
      viewModelScope.launch {
         delay(_timing.delayWrite)

         _carUcDelete(item.id)
            .onFailure { throwable ->
               _effects.showError(R.string.error_car_delete, throwable)
            }
         // On success the read model flow emits the list without this car.
      }
   }

   private fun findItem(carId: String): CarListItem? {
      val item = _stateFlow.value.cars.find { it.id == carId }
      if (item == null) _effects.launchShowError(R.string.error_car_not_found)
      return item
   }



   companion object { private const val TAG = "<-CarsViewModel" }
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
