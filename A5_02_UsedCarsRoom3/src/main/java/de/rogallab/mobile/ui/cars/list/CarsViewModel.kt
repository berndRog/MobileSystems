package de.rogallab.mobile.ui.cars.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.VisualRemovalController
import de.rogallab.mobile.ui.common.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CarsViewModel(
   private val _repository: ICarRepository,
   private val _personRepository: IPersonRepository,
) : ViewModel() {

   private val _state = MutableStateFlow(CarsUiState())
   val state: StateFlow<CarsUiState> = _state.asStateFlow()

   private val _events = Channel<CarsEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   private var _observeJob: Job? = null
   private val _visualRemoval = VisualRemovalController<Car>(Car::id)

   init {
      AppLogger.info(TAG, "init: observeCars()")
      observeCars()
      observePeople()
   }

   fun onIntent(intent: CarsIntent) {
      AppLogger.debug(TAG, "onIntent: $intent")
      when (intent) {
         CarsIntent.Create -> emitEvent(CarsEvent.NavigateToCreate)
         is CarsIntent.Open -> emitEvent(
            CarsEvent.NavigateToDetails(intent.carId)
         )
         is CarsIntent.Remove -> removeVisually(
            intent.car,
            intent.originalIndex,
         )
         is CarsIntent.Restore -> restoreVisually(
            intent.car,
            intent.originalIndex,
         )
         CarsIntent.Restored -> acknowledgeRestoredItem()
      }
   }

   private fun observeCars() {
      _observeJob?.cancel()
      _observeJob = viewModelScope.launch {
         _state.update { currentState -> currentState.copy(isLoading = true) }
         _repository.observeAll().collect { result ->
            result
               .onSuccess { databaseCars ->
                  val visibleCars = _visualRemoval.visibleItems(databaseCars)
                  _state.update { currentState ->
                     currentState.copy(
                        cars = visibleCars,
                        isLoading = false,
                     )
                  }
               }
               .onFailure {
                  _state.update { currentState -> currentState.copy(isLoading = false) }
                  emitEvent(
                     CarsEvent.ShowSnackbar(
                        uiText(R.string.error_cars_load)
                     )
                  )
               }
         }
      }
   }

   private fun observePeople() {
      viewModelScope.launch {
         _personRepository.observeAll().collect { result ->
            result
               .onSuccess { people ->
                  _state.update { currentState ->
                     currentState.copy(people = people)
                  }
               }
               .onFailure {
                  emitEvent(
                     CarsEvent.ShowSnackbar(
                        uiText(R.string.error_people_load)
                     )
                  )
               }
         }
      }
   }

   private fun removeVisually(car: Car, originalIndex: Int) {
      val result = _visualRemoval.remove(
         items = _state.value.cars,
         item = car,
         originalIndex = originalIndex,
      )
      _state.update { currentState ->
         currentState.copy(
            cars = result.items,
            restoredCarId = null,
         )
      }
      emitEvent(CarsEvent.RequestRemove(car, result.originalIndex))
   }

   private fun restoreVisually(car: Car, originalIndex: Int) {
      val restoredCars = _visualRemoval.restore(
         items = _state.value.cars,
         item = car,
         originalIndex = originalIndex,
      )
      _state.update { currentState ->
         currentState.copy(
            cars = restoredCars,
            restoredCarId = car.id,
         )
      }
   }

   private fun acknowledgeRestoredItem() {
      _state.update { currentState -> currentState.copy(restoredCarId = null) }
   }

   private fun emitEvent(event: CarsEvent) {
      viewModelScope.launch { _events.send(event) }
   }

   companion object {
      private const val TAG = "<-CarsViewModel"
   }
}
