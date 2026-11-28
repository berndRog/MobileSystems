package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.AppTiming
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.shared.domain.IEffectSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CarsViewModel(
   private val _repository: ICarRepository,
   private val _personRepository: IPersonRepository,
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
      observePeople()
   }

   fun onIntent(intent: CarsIntent) {
      Alog.d(TAG, "intent: $intent")
      when (intent) {
         CarsIntent.Create -> _effects.launchEmit(CarsEffect.NavigateTo(null))
         is CarsIntent.Detail -> _effects.launchEmit(CarsEffect.NavigateTo(intent.carId))
         is CarsIntent.RequestRemove -> requestRemove(intent.carId)
         is CarsIntent.ConfirmRemove -> confirmRemove(intent.carId)
      }
   }


   private fun requestRemove(carId: String) {
      val car = _stateFlow.value.cars.find { it.id == carId }
      if (car == null) {
         _effects.launchShowError(R.string.error_car_not_found)
         return
      }
      viewModelScope.launch {
         val message = _stringProvider.getString(
            R.string.message_car_remove_confirm,
            car.manufacturer,
            car.model,
         )
         val actionLabel = _stringProvider.getString(R.string.action_confirm)
         _effects.emit(CarsEffect.ConfirmRemove(message, actionLabel, car.id))
      }
   }

   private fun confirmRemove(carId: String) {
      val car = _stateFlow.value.cars.find { it.id == carId }
      if (car == null) {
         _effects.launchShowError(R.string.error_car_not_found)
         return
      }
      viewModelScope.launch {
         delay(_timing.delayWrite)

         _repository.remove(car)
            .onFailure { throwable -> _effects.showError(R.string.error_car_delete, throwable) }
      }
   }

   private fun observeCars() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {


         _stateFlow.update { state: CarsUiState -> state.copy(isLoading = true) }
         delay(_timing.delayRead)

         _repository.observeAll().collect { result: Result<List<Car>> ->
            result.onSuccess { cars ->
               _stateFlow.update { state: CarsUiState ->
                  state.copy(cars = cars, isLoading = false)
               }
            }.onFailure { throwable ->
               _stateFlow.update { state: CarsUiState -> state.copy(isLoading = false) }
               _effects.showError(R.string.error_cars_load, throwable)
            }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {

         delay(_timing.delayRead)

         _personRepository.observeAll().collect { result ->
            result.onSuccess { people ->
               _stateFlow.update { state: CarsUiState -> state.copy(people = people) }
            }.onFailure { throwable ->
               _effects.showError(R.string.error_people_load, throwable)
            }
         }
      }
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
 *
 * - CarsViewModel verwendet denselben UDF-Ablauf wie PeopleViewModel.
 * - Swipe-to-Delete entfernt kein Listenelement vorzeitig. Erst die bestätigte
 *   Snackbar-Aktion führt über ConfirmRemove zur Repository-Operation.
 * - Die Verkäuferliste wird separat beobachtet, damit die UI sellerId auf einen
 *   lesbaren Personennamen abbilden kann.
 */
