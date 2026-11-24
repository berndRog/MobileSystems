package de.rogallab.mobile.ui.tdrives.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.repositories.ITDriveRepository
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.common.DateTimeText
import de.rogallab.mobile.ui.people.create_detail.BackReason
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

// Write side: loads and saves the TDrive aggregate through the repository.
// Read side: driver and car selections come from the read models.
class TDriveViewModel(
   val tDriveId: String?,
   private val _tDriveRepository: ITDriveRepository,
   private val _personReadModel: IPersonReadModel,
   private val _carReadModel: ICarReadModel,
   private val _stringProvider: IStringProvider,
   private val _validator: TDriveValidator,
   private val _timing: AppTiming = AppTiming(),
   private val _effectDelegate: EffectDelegate<TDriveEffect>,
) : ViewModel(), IEffectSource<TDriveEffect> by _effectDelegate {

   private val _tDriveId = tDriveId?.takeUnless(String::isBlank)
   private val _isNew = _tDriveId == null
   private val _localTimeZone = TimeZone.currentSystemDefault()
   private var _isSaving = false

   // A new test drive starts now, truncated to minutes.
   private val _defaultStartInput: String =
      Clock.System.now()
         .toLocalDateTime(_localTimeZone)
         .let { now -> LocalDateTime(now.date, LocalTime(now.hour, now.minute)) }
         .let(DateTimeText::format)

   // A new test drive starts with an empty form; an existing one is loaded first.
   private val _initialState =
      if (_isNew) TDriveUiState(form = TDriveForm(startInput = _defaultStartInput), isNew = true)
      else TDriveUiState(isNew = false, isLoading = true)

   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<TDriveUiState> = _stateFlow.asStateFlow()

   init {
      observePeople()
      observeCars()
      if (!_isNew) loadTDrive(_tDriveId!!)
   }

   fun onIntent(intent: TDriveIntent) {
      when (intent) {
         is TDriveIntent.PersonChanged -> updateForm { it.copy(personId = intent.personId) }
         is TDriveIntent.CarChanged -> updateForm { it.copy(carId = intent.carId) }
         is TDriveIntent.StartChanged -> updateForm { it.copy(startInput = intent.value) }
         is TDriveIntent.CompletedChanged -> updateForm { it.copy(isCompleted = intent.value) }
         TDriveIntent.Save -> save()
         TDriveIntent.Cancel -> navigateBack(BackReason.Cancel)
      }
   }

   // Driver selection: only IPerson... types access the Person table.
   private fun observePeople() {
      viewModelScope.launch {
         delay(_timing.delayRead)
         try {
            _personReadModel.observeSelectItems().collect { result: Result<List<PersonSelectItem>> ->
               val people = result.getOrThrow()
               _stateFlow.update { state: TDriveUiState -> state.copy(people = people) }
            }
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            showErrorNow(_stringProvider.getString(R.string.error_people_load))
         }
      }
   }

   // Car selection: only ICar... types access the Car table.
   private fun observeCars() {
      viewModelScope.launch {
         delay(_timing.delayRead)
         try {
            _carReadModel.observeSelectItems().collect { result: Result<List<CarSelectItem>> ->
               val cars = result.getOrThrow()
               _stateFlow.update { state: TDriveUiState -> state.copy(cars = cars) }
            }
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            showErrorNow(_stringProvider.getString(R.string.error_cars_load))
         }
      }
   }

   private fun loadTDrive(id: String) {
      viewModelScope.launch {
         try {
            delay(_timing.delayRead)

            val tDrive = _tDriveRepository.findById(id).getOrThrow()
               ?: throw NoSuchElementException("Test drive $id not found")

            // Domain -> form: the start instant becomes local text.
            _stateFlow.update { state: TDriveUiState ->
               state.copy(form = tDrive.toTDriveForm(_localTimeZone))
            }
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: NoSuchElementException) {
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_not_found))
         }
         catch (e: Exception) {
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_load))
         }
         finally {
            _stateFlow.update { state: TDriveUiState -> state.copy(isLoading = false) }
         }
      }
   }

   // Raw input is stored unchanged; normalization happens on Save.
   private fun updateForm(transform: (TDriveForm) -> TDriveForm) {
      _stateFlow.update { state: TDriveUiState -> state.copy(form = transform(state.form)) }
   }

   private fun save() {
      if (_isSaving || _stateFlow.value.isLoading) return

      // Normalize all form values and show them in the UI as well.
      val form: TDriveForm = _stateFlow.value.form.normalized()
      _stateFlow.update { state: TDriveUiState -> state.copy(form = form) }

      // Validate the form before creating a domain object.
      val error = _validator.validateTDrive(form)
      if (error != null) {
         showError(error)
         return
      }

      // Only a valid form becomes a domain test drive.
      val tDrive: TDrive = form.toTDrive(_localTimeZone)
      _isSaving = true

      viewModelScope.launch {
         try {
            delay(_timing.delayWrite)

            // No images involved, so no use case is needed for saving.
            if (_isNew) _tDriveRepository.create(tDrive).getOrThrow()
            else _tDriveRepository.update(tDrive).getOrThrow()

            _effectDelegate.emit(
               TDriveEffect.ShowMessage(_stringProvider.getString(R.string.message_test_drive_saved))
            )
            _effectDelegate.emit(TDriveEffect.NavigateBack(BackReason.Save))
         }
         catch (e: CancellationException) {
            throw e
         }
         catch (e: Exception) {
            // Includes the unique index violation (same driver, car and start).
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_save))
         }
         finally {
            _isSaving = false
         }
      }
   }

   private fun navigateBack(reason: BackReason) {
      viewModelScope.launch { _effectDelegate.emit(TDriveEffect.NavigateBack(reason)) }
   }

   private fun showError(message: String) {
      viewModelScope.launch { showErrorNow(message) }
   }

   private suspend fun showErrorNow(message: String) {
      _effectDelegate.emit(TDriveEffect.ShowError(message))
   }
}
