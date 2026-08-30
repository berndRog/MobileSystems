package de.rogallab.mobile.ui.people.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.normalizedImagePath
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import de.rogallab.mobile.ui.people.normalized
import de.rogallab.mobile.ui.people.toNullableInput
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonViewModel(
   arguments: PersonVmArgs,
   private val _repository: IPersonRepository,
   private val _validator: PersonValidator,
) : ViewModel() {

   private val _personId = arguments.personId?.takeUnless(String::isBlank)
   private val _isNew = _personId == null

   private val _initialState =
      if (_isNew) {
         PersonUiState(
            person = Person(id = UUID.randomUUID().toString()),
            isNew = true,
         )
      }
      else {
         PersonUiState(isNew = false, isLoading = true)
      }

   private val _state = MutableStateFlow(_initialState)
   val state: StateFlow<PersonUiState> = _state.asStateFlow()

   private val _effects = Channel<PersonEffect>(Channel.BUFFERED)
   val effects = _effects.receiveAsFlow()

   private var _originalImagePath: String? = null
   private var _imageOwnershipTransferred = false

   init {
      AppLogger.debug(TAG, "init: isNew=$_isNew, personId=$_personId")
      if (!_isNew) loadPerson()
   }

   fun onIntent(intent: PersonIntent) {
      AppLogger.debug(TAG, "onIntent: $intent")
      when (intent) {
         is PersonIntent.FirstNameChanged -> changeFirstName(intent.value)
         is PersonIntent.LastNameChanged -> changeLastName(intent.value)
         is PersonIntent.EmailChanged -> changeEmail(intent.value)
         is PersonIntent.PhoneChanged -> changePhone(intent.value)
         is PersonIntent.ImageChanged -> changeImage(intent.value)
         is PersonIntent.ImageStorageFailed -> showSnackbar(intent.message)
         PersonIntent.Save -> validateAndRequestSave()
         PersonIntent.Cancel -> cancelEditing()
      }
   }

   private fun loadPerson() {
      val personId = _personId ?: return
      deleteUnsavedReplacementImage()

      viewModelScope.launch {
         _state.update { currentState ->
            currentState.copy(person = null, isLoading = true)
         }

         _repository.findById(personId)
            .onSuccess { person ->
               if (person == null) {
                  _state.update { currentState ->
                     currentState.copy(isLoading = false)
                  }
                  showSnackbarAndNavigateBack(
                     uiText(R.string.error_person_not_found)
                  )
                  return@onSuccess
               }

               _originalImagePath = person.imagePath
               _imageOwnershipTransferred = false
               _state.update { currentState ->
                  currentState.copy(person = person, isLoading = false)
               }
            }
            .onFailure {
               _state.update { currentState ->
                  currentState.copy(isLoading = false)
               }
               showSnackbarAndNavigateBack(
                  uiText(R.string.error_person_load)
               )
            }
      }
   }

   private fun changeFirstName(value: String) {
      updatePerson { person -> person.copy(firstName = value) }
   }

   private fun changeLastName(value: String) {
      updatePerson { person -> person.copy(lastName = value) }
   }

   private fun changeEmail(value: String) {
      updatePerson { person ->
         person.copy(email = value.toNullableInput())
      }
   }

   private fun changePhone(value: String) {
      updatePerson { person ->
         person.copy(phone = value.toNullableInput())
      }
   }

   private fun changeImage(value: String?) {
      val previousImagePath = _state.value.person?.imagePath
      val newImagePath = value.normalizedImagePath()
      if (
         previousImagePath != null &&
         previousImagePath != _originalImagePath &&
         previousImagePath != newImagePath
      ) {
         deleteImageFromAppStorage(previousImagePath)
      }
      updatePerson { person -> person.copy(imagePath = newImagePath) }
   }

   private fun updatePerson(transform: (Person) -> Person) {
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = transform(person))
      }
   }

   private fun validateAndRequestSave() {
      val person = _state.value.person?.normalized() ?: return
      _state.update { currentState -> currentState.copy(person = person) }

      val errorMessage = _validator.validatePerson(person)
      if (errorMessage != null) {
         showSnackbar(UiText.Resolved(errorMessage))
         return
      }

      _imageOwnershipTransferred = true
      viewModelScope.launch {
         _effects.send(PersonEffect.RequestSave(person, _isNew))
      }
   }

   private fun cancelEditing() {
      deleteUnsavedReplacementImage()
      navigateBack()
   }

   private fun showSnackbar(message: UiText) {
      viewModelScope.launch {
         _effects.send(PersonEffect.ShowSnackbar(message))
      }
   }

   private fun showSnackbarAndNavigateBack(message: UiText) {
      viewModelScope.launch {
         _effects.send(PersonEffect.ShowSnackbar(message))
         _effects.send(PersonEffect.NavigateBack)
      }
   }

   private fun navigateBack() {
      viewModelScope.launch { _effects.send(PersonEffect.NavigateBack) }
   }

   private fun deleteUnsavedReplacementImage() {
      if (_imageOwnershipTransferred) return
      val currentImagePath = _state.value.person?.imagePath
      if (currentImagePath != null && currentImagePath != _originalImagePath) {
         deleteImageFromAppStorage(currentImagePath)
      }
   }

   override fun onCleared() {
      deleteUnsavedReplacementImage()
      super.onCleared()
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}
