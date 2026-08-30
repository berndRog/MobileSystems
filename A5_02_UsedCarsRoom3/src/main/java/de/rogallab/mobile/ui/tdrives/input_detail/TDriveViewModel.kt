package de.rogallab.mobile.ui.tdrives.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TDriveViewModel(
   arguments: TDriveVmArgs,
   private val _tDriveRepository: ITDriveRepository,
   private val _personRepository: IPersonRepository,
   private val _carRepository: ICarRepository,
   private val _validator: TDriveValidator,
) : ViewModel() {

   private val _tDriveId = arguments.tDriveId?.takeUnless(String::isBlank)
   private val _isNew = _tDriveId == null
   private val _defaultStart = LocalDateTime.now().withSecond(0).withNano(0)
   private val _state = MutableStateFlow(
      if (_isNew) {
         val tDrive = TDrive(
            id = UUID.randomUUID().toString(),
            start = _defaultStart,
         )
         TDriveUiState(
            tDrive = tDrive,
            startInput = DateTimeText.format(tDrive.start),
            isNew = true,
         )
      }
      else {
         TDriveUiState(isNew = false, isLoading = true)
      }
   )
   val state: StateFlow<TDriveUiState> = _state.asStateFlow()

   private val _events = Channel<TDriveEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   init {
      AppLogger.debug(TAG, "init: isNew=$_isNew, testDriveId=$_tDriveId")
      observePeople()
      observeCars()
      if (!_isNew) loadTestDrive()
   }

   fun onIntent(intent: TDriveIntent) {
      when (intent) {
         is TDriveIntent.PersonChanged -> updateTestDrive { testDrive ->
            testDrive.copy(personId = intent.personId)
         }
         is TDriveIntent.CarChanged -> updateTestDrive { testDrive ->
            testDrive.copy(carId = intent.carId)
         }
         is TDriveIntent.StartChanged -> _state.update { currentState ->
            currentState.copy(startInput = intent.value)
         }
         is TDriveIntent.NotesChanged -> updateTestDrive { testDrive ->
            testDrive.copy(
               notes = intent.value.trim().takeUnless(String::isBlank)
            )
         }
         is TDriveIntent.CompletedChanged -> updateTestDrive { testDrive ->
            testDrive.copy(isCompleted = intent.value)
         }
         TDriveIntent.Save -> validateAndRequestSave()
         TDriveIntent.Cancel -> emitEvent(TDriveEvent.NavigateBack)
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
                  showSnackbar(uiText(R.string.error_people_load))
               }
         }
      }
   }

   private fun observeCars() {
      viewModelScope.launch {
         _carRepository.observeAll().collect { result ->
            result
               .onSuccess { cars ->
                  _state.update { currentState ->
                     currentState.copy(cars = cars)
                  }
               }
               .onFailure {
                  showSnackbar(uiText(R.string.error_cars_load))
               }
         }
      }
   }

   private fun loadTestDrive() {
      val testDriveId = _tDriveId ?: return
      viewModelScope.launch {
         _tDriveRepository.findById(testDriveId)
            .onSuccess { testDrive ->
               if (testDrive == null) {
                  _state.update { currentState -> currentState.copy(isLoading = false) }
                  showSnackbarAndNavigateBack(
                     uiText(R.string.error_test_drive_not_found)
                  )
               }
               else {
                  _state.update { currentState ->
                     currentState.copy(
                        tDrive = testDrive,
                        startInput = DateTimeText.format(testDrive.start),
                        isLoading = false,
                     )
                  }
               }
            }
            .onFailure {
               _state.update { currentState -> currentState.copy(isLoading = false) }
               showSnackbarAndNavigateBack(uiText(R.string.error_test_drive_load))
            }
      }
   }

   private fun updateTestDrive(transform: (TDrive) -> TDrive) {
      _state.update { currentState ->
         val testDrive = currentState.tDrive ?: return@update currentState
         currentState.copy(tDrive = transform(testDrive))
      }
   }

   private fun validateAndRequestSave() {
      val currentState = _state.value
      val testDrive = currentState.tDrive ?: return
      val start = _validator.parseStart(currentState.startInput)
      if (start == null) {
         showSnackbar(UiText.Resolved(
            _validator.validateStart(currentState.startInput).orEmpty()
         ))
         return
      }
      val normalizedTestDrive = testDrive.copy(
         start = start,
         notes = testDrive.notes?.trim()?.takeUnless(String::isBlank),
      )
      val errorMessage = _validator.validateTestDrive(
         normalizedTestDrive,
         currentState.startInput,
      )
      if (errorMessage != null) {
         showSnackbar(UiText.Resolved(errorMessage))
         return
      }
      _state.update { state -> state.copy(tDrive = normalizedTestDrive) }
      emitEvent(TDriveEvent.RequestSave(normalizedTestDrive, _isNew))
   }

   private fun showSnackbar(message: UiText) {
      emitEvent(TDriveEvent.ShowSnackbar(message))
   }

   private fun showSnackbarAndNavigateBack(message: UiText) {
      viewModelScope.launch {
         _events.send(TDriveEvent.ShowSnackbar(message))
         _events.send(TDriveEvent.NavigateBack)
      }
   }

   private fun emitEvent(event: TDriveEvent) {
      viewModelScope.launch { _events.send(event) }
   }

   companion object {
      private const val TAG = "<-TDriveViewModel"
   }
}
