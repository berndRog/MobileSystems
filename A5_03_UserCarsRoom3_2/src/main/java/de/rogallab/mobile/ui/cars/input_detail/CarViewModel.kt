package de.rogallab.mobile.ui.cars.input_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.PersonSelectItem
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm
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
import kotlin.coroutines.cancellation.CancellationException
import de.rogallab.mobile.shared.R as SharedR

class CarViewModel(
   val carId: String?,
   private val _carRepository: ICarRepository,
   private val _personReadModel: IPersonReadModel,
   private val _stringProvider: IStringProvider,
   private val _validator: CarValidator,
   private val _images: IImageEditDelegate,
   private val _carUcCreate: CarUcCreate,
   private val _carUcUpdate: CarUcUpdate,
   private val _effectDelegate: EffectDelegate<CarEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<CarEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<CarEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { CarEffect.ShowError(it) },
      _messageEffect = { CarEffect.ShowMessage(it) },
      _tag = "<-CarViewModel",
   )

   private val _carId = carId?.takeUnless(String::isBlank)
   private val _isNew = _carId == null
   // Block further actions until navigation handles the closing effect.
   private var _isClosing = false
   private var _peopleJob: Job? = null

   // A new car starts with an empty form; an existing car is loaded first.
   private val _initialState =
      if (_isNew) CarUiState(form = CarForm(), isNew = true)
      else CarUiState(isNew = false, isLoading = true)

   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<CarUiState> = _stateFlow.asStateFlow()

   init {
      observePeople()
      if (_isNew) _images.start(emptyList())
      else loadCar(_carId!!)
   }

   fun onIntent(intent: CarIntent) {
      // Text and selection edits stay available while images are processed.
      if ((_stateFlow.value.isSaving || _isClosing) &&
         (intent is CarIntent.ManufacturerChanged ||
            intent is CarIntent.ModelChanged ||
            intent is CarIntent.PriceChanged ||
            intent is CarIntent.SellerChanged)) return

      when (intent) {
         is CarIntent.ManufacturerChanged -> changeManufacturer(intent.value)

         is CarIntent.ModelChanged -> changeModel(intent.value)

         is CarIntent.PriceChanged -> changePrice(intent.value)

         is CarIntent.SellerChanged -> changeSeller(intent.personId)

         is CarIntent.GalleryImagesSelected ->
            storeGalleryImages(intent.sourceUris)

         is CarIntent.CameraImageTaken ->
            storeCameraImage(intent.imagePath)

         is CarIntent.ImageRemoved ->
            removeImage(intent.imagePath)

         is CarIntent.ImageFailed ->
            _effects.launchShowError(intent.message)

         CarIntent.RetryLoad ->
            if (!_isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is CarLoadFailure.Failed) {
               loadCar(_carId!!)
            }
         CarIntent.RetryPeople ->
            if (!_stateFlow.value.isPeopleLoading && _stateFlow.value.peopleLoadFailure != null) {
               observePeople()
            }
         CarIntent.Save -> save()
         CarIntent.Cancel -> cancel()
      }
   }

   // Retry is gated by isLoading; the ViewModel always edits the original id.
   private fun loadCar(id: String) {
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      viewModelScope.launch {
         try {
            delay(_timing.delayRead)
            val car = _carRepository.findById(id).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (car == null) {
               _stateFlow.update {
                  it.copy(isLoading = false, loadFailure = CarLoadFailure.NotFound(
                     _stringProvider.getString(R.string.error_car_not_found)))
               }
            } else {
               _images.start(car.imagePaths)
               _stateFlow.update {
                  it.copy(form = car.toCarForm(),
                     isLoading = false, loadFailure = null)
               }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false, loadFailure = CarLoadFailure.Failed(
                  _stringProvider.getString(R.string.error_car_load)))
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

   // Raw input is stored unchanged; normalization happens on Save.
   private fun changeManufacturer(manufacturer: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(manufacturer = manufacturer))
      }

   private fun changeModel(model: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(model = model))
      }

   private fun changePrice(priceInput: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(priceInput = priceInput))
      }

   private fun changeSeller(personId: String?) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(personId = personId))
      }

   private fun changeImagePaths(imagePaths: List<String>) {
      if (_stateFlow.value.isSaving || _isClosing) return
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(imagePaths = imagePaths))
      }
   }

   // All image entry points run on Main, like the other UI intents.
   private fun beginImageChange(): Boolean {
      val state = _stateFlow.value
      if (state.isEditingImages || state.isSaving || state.isLoading ||
         state.loadFailure != null || _isClosing) return false
      _stateFlow.update { it.copy(isEditingImages = true) }
      return true
   }

   private fun changeImages(operation: suspend () -> Result<ImageChange>) {
      if (!beginImageChange()) return
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            currentCoroutineContext().ensureActive()
            operation()
               .onSuccess { change ->
                  currentCoroutineContext().ensureActive()
                  change.paths?.let { paths -> changeImagePaths(paths) }
                  if (change.failedImports > 0) {
                     _effects.showError(SharedR.string.error_image_save)
                  }
               }
               .onFailure { error ->
                  currentCoroutineContext().ensureActive()
                  if (error is CancellationException) throw error
                  _effects.showError(SharedR.string.error_image_save, error)
               }
         } finally {
            _stateFlow.update { it.copy(isEditingImages = false) }
         }
      }
   }

   private fun storeGalleryImages(uris: List<Uri>) {
      if (uris.isNotEmpty()) changeImages { _images.importGallery(uris) }
   }

   private fun storeCameraImage(path: String) {
      val state = _stateFlow.value
      if (state.isEditingImages || state.isSaving || state.isLoading ||
         state.loadFailure != null || _isClosing) {
         viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _images.rejectCamera(path).onFailure { error ->
               if (error is CancellationException) throw error
               if (!_isClosing) _effects.showError(SharedR.string.error_image_save)
            }
         }
      } else changeImages { _images.acceptCamera(path) }
   }

   private fun removeImage(path: String) = changeImages { _images.remove(path) }

   private fun save() {
      val state = _stateFlow.value
      if (state.isSaving || state.isEditingImages || _isClosing || state.isLoading || state.loadFailure != null) return

      // This immutable snapshot is the only input to the write operation.
      val form = state.form.normalized().copy(imagePaths = state.form.imagePaths.toList())
      _stateFlow.update { it.copy(form = form) }
      val error = _validator.validateCar(form)
      if (error != null) {
         _effects.launchShowError(error)
         return
      }
      val car: Car = form.toCar()

      // Mark the attempt synchronously, before the coroutine can be scheduled.
      _stateFlow.update { it.copy(isSaving = true) }
      // Enter try/finally immediately, even if the scope is cancelled before dispatch.
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            delay(_timing.delayWrite)
            val result =
               if (_isNew) _carUcCreate(car)
               else _carUcUpdate(car)

            result.getOrThrow()
            currentCoroutineContext().ensureActive()
            _isClosing = true
            _effects.showMessage(R.string.message_car_saved, car.displayName)
            _effects.emit(CarEffect.NavigateBack(BackReason.Save))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _effects.showError(R.string.error_car_save, e)
         } finally {
            _stateFlow.update { it.copy(isSaving = false) }
         }
      }
   }

   private fun cancel() {
      if (_stateFlow.value.isSaving || _stateFlow.value.isEditingImages || _isClosing) return
      // Once Cancel is accepted, Save must not race the pending navigation.
      _isClosing = true
      viewModelScope.launch {
         try {
            _images.discard().getOrThrow()
            _effects.emit(CarEffect.NavigateBack(BackReason.Cancel))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            _isClosing = false
            _effects.showError(SharedR.string.error_image_save, e)
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
