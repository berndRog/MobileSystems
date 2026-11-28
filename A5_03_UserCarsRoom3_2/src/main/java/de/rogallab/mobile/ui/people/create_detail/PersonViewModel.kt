package de.rogallab.mobile.ui.people.create_detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.IEffectSource
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.EffectDispatcher
import kotlinx.coroutines.CoroutineStart
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

class PersonViewModel(
   val personId: String?,
   private val _repository: IPersonRepository,
   private val _carReadModel: ICarReadModel,
   private val _stringProvider: IStringProvider,
   private val _validator: PersonValidator,
   private val _images: IImageEditDelegate,
   private val _personUcCreate: PersonUcCreate,
   private val _personUcUpdate: PersonUcUpdate,
   private val _effectDelegate: EffectDelegate<PersonEffect>,
   private val _timing: AppTiming = AppTiming(),
) : ViewModel(), IEffectSource<PersonEffect> by _effectDelegate {

   // Reuse the existing scope and transport; suspend calls preserve effect order.
   private val _effects = EffectDispatcher<PersonEffect>(
      _scope = viewModelScope,
      _effectDelegate = _effectDelegate,
      _stringProvider = _stringProvider,
      _errorEffect = { PersonEffect.ShowError(it) },
      _messageEffect = { PersonEffect.ShowMessage(it) },
      _tag = "<-PersonViewModel",
   )

   private val _personId = personId?.toNullableInput()
   private val _isNew = _personId == null
   // Block further actions until navigation handles the closing effect.
   private var _isClosing = false

   // A new person starts with an empty form; an existing person is loaded first.
   private val _initialState =
      if (_isNew) PersonUiState(form = PersonForm(), isNew = true)
      else PersonUiState(isNew = false, isLoading = true)

   private val _stateFlow = MutableStateFlow(_initialState)
   val stateFlow: StateFlow<PersonUiState> = _stateFlow.asStateFlow()

   init {
      if (_isNew) _images.start(emptyList())
      else loadPerson(_personId!!)
   }

   fun onIntent(intent: PersonIntent) {
      // Text and selection edits stay available while images are processed.
      if ((_stateFlow.value.isSaving || _isClosing) &&
         (intent is PersonIntent.FirstNameChange ||
            intent is PersonIntent.LastNameChange ||
            intent is PersonIntent.EmailChange ||
            intent is PersonIntent.PhoneChange)) return

      when (intent) {
         is PersonIntent.FirstNameChange -> changeFirstName(intent.firstName)
         is PersonIntent.LastNameChange -> changeLastName(intent.lastName)
         is PersonIntent.EmailChange -> changeEmail(intent.email)
         is PersonIntent.PhoneChange -> changePhone(intent.phone)
         is PersonIntent.GalleryImageSelected -> storeGalleryImage(intent.sourceUri)
         is PersonIntent.CameraImageTaken -> storeCameraImage(intent.imagePath)
         is PersonIntent.RemoveImage -> removeImage(intent.imagePath)
         is PersonIntent.ImageFailed -> _effects.launchShowError(intent.message)
         PersonIntent.RetryLoad ->
            if (!_isNew && !_stateFlow.value.isLoading &&
               _stateFlow.value.loadFailure is PersonLoadFailure.Failed) {
               loadPerson(_personId!!)
            }
         PersonIntent.CarsRequested -> loadCars()
         PersonIntent.RetryCars ->
            if (!_stateFlow.value.isCarsLoading && _stateFlow.value.carsLoadFailure != null) {
               loadCars()
            }
         PersonIntent.Save -> save()
         PersonIntent.Cancel -> cancel()
      }
   }

   // Retry is gated by isLoading; the ViewModel always edits the original id.
   private fun loadPerson(id: String) {
      _stateFlow.update { it.copy(isLoading = true, loadFailure = null) }
      viewModelScope.launch {
         try {
            delay(_timing.delayRead)
            val person = _repository.findById(id).getOrThrow()
            currentCoroutineContext().ensureActive()
            if (person == null) {
               _stateFlow.update {
                  it.copy(isLoading = false, loadFailure = PersonLoadFailure.NotFound(
                     _stringProvider.getString(R.string.error_person_not_found)))
               }
            } else {
               _images.start(listOfNotNull(person.imagePath))
               _stateFlow.update {
                  it.copy(form = person.toPersonForm(),
                     isLoading = false, loadFailure = null)
               }
            }
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _stateFlow.update {
               it.copy(isLoading = false, loadFailure = PersonLoadFailure.Failed(
                  _stringProvider.getString(R.string.error_person_load)))
            }
         }
      }
   }

   // Offered cars are a snapshot requested by the user, not a continuous selection flow.
   private fun loadCars() {
      val id = _personId ?: return
      val state = _stateFlow.value
      if (state.isCarsLoading || state.isLoading || state.loadFailure != null ||
         state.isSaving || state.isEditingImages || _isClosing) return
      _stateFlow.update { it.copy(isCarsLoading = true, carsLoadFailure = null) }
      viewModelScope.launch {
         try {
            delay(_timing.delayRead)
            val cars = _carReadModel.findBySellerId(id).getOrThrow()
            currentCoroutineContext().ensureActive()
            _stateFlow.update { it.copy(cars = cars, isCarsLoading = false) }
            if (!_isClosing) _effects.emit(PersonEffect.ShowCars)
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            val error = _stringProvider.getString(R.string.error_cars_load)
            _stateFlow.update { it.copy(isCarsLoading = false, carsLoadFailure = error) }
            _effects.showError(error, e)
         }
      }
   }

   // Raw input is stored unchanged; normalization happens on Save.
   private fun changeFirstName(firstName: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(firstName = firstName))
      }

   private fun changeLastName(lastName: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(lastName = lastName))
      }

   private fun changeEmail(email: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(email = email))
      }

   private fun changePhone(phone: String) =
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(phone = phone))
      }

   private fun changeImagePath(imagePath: String?) {
      if (_stateFlow.value.isSaving || _isClosing) return
      _stateFlow.update { state ->
         state.copy(form = state.form.copy(imagePath = imagePath))
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
                  change.paths?.let { paths -> changeImagePath(paths.firstOrNull()) }
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

   private fun storeGalleryImage(uri: Uri) =
      changeImages { _images.importGallery(listOf(uri)) }

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

   private fun removeImage(path: String?) {
      val imagePath = path ?: _stateFlow.value.form.imagePath ?: return
      changeImages { _images.remove(imagePath) }
   }

   private fun save() {
      val state = _stateFlow.value
      if (state.isSaving || state.isEditingImages || _isClosing || state.isLoading || state.loadFailure != null) return

      // This immutable snapshot is the only input to the write operation.
      val form = state.form.normalized()
      _stateFlow.update { it.copy(form = form) }
      val error = _validator.validatePerson(form)
      if (error != null) {
         _effects.launchShowError(error)
         return
      }
      val person: Person = form.toPerson()

      // Mark the attempt synchronously, before the coroutine can be scheduled.
      _stateFlow.update { it.copy(isSaving = true) }
      // Enter try/finally immediately, even if the scope is cancelled before dispatch.
      viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
         try {
            delay(_timing.delayWrite)
            val result =
               if (_isNew) _personUcCreate(person)
               else _personUcUpdate(person)

            result.getOrThrow()
            currentCoroutineContext().ensureActive()
            _isClosing = true
            _effects.showMessage(R.string.message_person_saved, person.fullName)
            _effects.emit(PersonEffect.NavigateBack(BackReason.Save))
         } catch (e: CancellationException) {
            throw e
         } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            _effects.showError(R.string.error_person_save, e)
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
            _effects.emit(PersonEffect.NavigateBack(BackReason.Cancel))
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
