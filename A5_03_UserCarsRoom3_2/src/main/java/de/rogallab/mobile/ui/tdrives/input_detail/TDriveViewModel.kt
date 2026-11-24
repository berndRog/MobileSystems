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
import kotlinx.coroutines.CoroutineStart
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
   // Block further actions until navigation handles the closing effect.
   private var _isClosing = false
   private var _peopleJob: Job? = null
   private var _carsJob: Job? = null

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
         TDriveIntent.RetryLoad ->
            if (!_isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is TDriveLoadFailure.Failed) {
               loadTDrive(_tDriveId!!)
            }
         TDriveIntent.RetryPeople ->
            if (!_stateFlow.value.isPeopleLoading && _stateFlow.value.peopleLoadFailure != null) {
               observePeople()
            }
         TDriveIntent.RetryCars ->
            if (!_stateFlow.value.isCarsLoading && _stateFlow.value.carsLoadFailure != null) {
               observeCars()
            }
         TDriveIntent.Save -> save()
         TDriveIntent.Cancel -> cancel()
      }
   }

   // Retry is gated by isLoading; the ViewModel always edits the original id.
   private fun loadTDrive(id: String) {
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      viewModelScope.launch {
         try {
            delay(_timing.delayRead)
            val tDrive = _tDriveRepository.findById(id).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (tDrive == null) {
               _stateFlow.update {
                  it.copy(isLoading = false, loadFailure = TDriveLoadFailure.NotFound(
                     _stringProvider.getString(R.string.error_test_drive_not_found)))
               }
            } else {
               _stateFlow.update {
                  it.copy(form = tDrive.toTDriveForm(_localTimeZone),
                     isLoading = false, loadFailure = null)
               }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false, loadFailure = TDriveLoadFailure.Failed(
                  _stringProvider.getString(R.string.error_test_drive_load)))
            }
         }
      }
   }

   // Selection updates must not reload the edited form.
   private fun observePeople() {
      val previousJob = _peopleJob
      // Set loading before launch: repeated Retry intents cannot start another attempt.
      _stateFlow.update { it.copy(isPeopleLoading = true, peopleLoadFailure = null) }
      _peopleJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _personReadModel.observeSelectItems().collect { result: Result<List<PersonSelectItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(people = items, isPeopleLoading = false, peopleLoadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isPeopleLoading = false,
                           peopleLoadFailure = _stringProvider.getString(R.string.error_people_load))
                     }
                     // Stop this collector, not a possibly newer job stored in the field.
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // Also handle a Flow that throws instead of emitting Result.failure.
            _stateFlow.update {
               it.copy(isPeopleLoading = false,
                  peopleLoadFailure = _stringProvider.getString(R.string.error_people_load))
            }
         }
      }
   }

   // Retry this selection independently of the driver selection.
   private fun observeCars() {
      val previousJob = _carsJob
      // Set loading before launch: repeated Retry intents cannot start another attempt.
      _stateFlow.update { it.copy(isCarsLoading = true, carsLoadFailure = null) }
      _carsJob = viewModelScope.launch {
         previousJob?.cancelAndJoin()
         try {
            delay(_timing.delayRead)
            _carReadModel.observeSelectItems().collect { result: Result<List<CarSelectItem>> ->
               currentCoroutineContext().ensureActive()
               result
                  .onSuccess { items ->
                     _stateFlow.update {
                        it.copy(cars = items, isCarsLoading = false, carsLoadFailure = null)
                     }
                  }
                  .onFailure { throwable ->
                     if (throwable is CancellationException) throw throwable
                     _stateFlow.update {
                        it.copy(isCarsLoading = false,
                           carsLoadFailure = _stringProvider.getString(R.string.error_cars_load))
                     }
                     // Stop this collector, not a possibly newer job stored in the field.
                     currentCoroutineContext().cancel()
                     currentCoroutineContext().ensureActive()
                  }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            // Also handle a Flow that throws instead of emitting Result.failure.
            _stateFlow.update {
               it.copy(isCarsLoading = false,
                  carsLoadFailure = _stringProvider.getString(R.string.error_cars_load))
            }
         }
      }
   }

   // Raw input is stored unchanged; normalization happens on Save.
   private fun updateForm(transform: (TDriveForm) -> TDriveForm) {
      if (_stateFlow.value.isSaving || _isClosing) return
      _stateFlow.update { it.copy(form = transform(it.form)) }
   }

   private fun save() {
      val state = _stateFlow.value
      if (state.isSaving || _isClosing || state.isLoading || state.loadFailure != null) return

      // This immutable snapshot is the only input to the write operation.
      val form = state.form.normalized()
      _stateFlow.update { it.copy(form = form) }
      val error = _validator.validateTDrive(form)
      if (error != null) {
         showError(error)
         return
      }
      val tDrive: TDrive = form.toTDrive(_localTimeZone)

      // Mark the attempt synchronously, before the coroutine can be scheduled.
      _stateFlow.update { it.copy(isSaving = true) }
      // Enter try/finally immediately, even if the scope is cancelled before dispatch.
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            delay(_timing.delayWrite)
            val result =
               if (_isNew) _tDriveRepository.create(tDrive)
               else _tDriveRepository.update(tDrive)

            result.getOrThrow()
            currentCoroutineContext().ensureActive()
            _isClosing = true
            _effectDelegate.emit(
               TDriveEffect.ShowMessage(
                  _stringProvider.getString(R.string.message_test_drive_saved)
               )
            )
            _effectDelegate.emit(TDriveEffect.NavigateBack(BackReason.Save))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_save))
         } finally {
            _stateFlow.update { it.copy(isSaving = false) }
         }
      }
   }

   private fun cancel() {
      if (_stateFlow.value.isSaving || _isClosing) return
      // Once Cancel is accepted, Save must not race the pending navigation.
      _isClosing = true
      viewModelScope.launch {
         try {
            _effectDelegate.emit(TDriveEffect.NavigateBack(BackReason.Cancel))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            _isClosing = false
            showErrorNow(_stringProvider.getString(R.string.error_test_drive_save))
         }
      }
   }

   private fun showError(message: String) {
      viewModelScope.launch { showErrorNow(message) }
   }

   private suspend fun showErrorNow(message: String) {
      _effectDelegate.emit(TDriveEffect.ShowError(message))
   }
}
