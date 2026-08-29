package de.rogallab.mobile.ui.people.input_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import de.rogallab.mobile.ui.people.normalized
import de.rogallab.mobile.ui.people.toNullableInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Shared ViewModel for creating and editing a person.
//
// Each navigation entry owns its own PersonViewModel instance. The optional
// personId determines the workflow of that instance:
//
// - personId == null: create a new person,
// - personId != null: load and edit an existing person.
//
// The public contract is identical in both cases: PersonUiState, PersonIntent
// and PersonEvent. Only the private save behavior depends on the workflow.
class PersonViewModel(
   arguments: PersonVmArgs,
   private val _repository: IPersonRepository,
   private val _validator: PersonValidator,
) : ViewModel() {

   private val _personId = arguments.personId
      ?.takeUnless(String::isBlank)

   private val _isNew = _personId == null

   private val _initialState = if (_isNew) {
      PersonUiState(
         person = Person(id = UUID.randomUUID().toString()),
         isNew = true)
   }
   else {
      PersonUiState(isNew = false, isLoading = true)
   }

   private val _state = MutableStateFlow(_initialState)
   val state: StateFlow<PersonUiState> = _state.asStateFlow()

   private val _events = Channel<PersonEvent>(Channel.BUFFERED)
   val events = _events.receiveAsFlow()

   // For an existing person, this path belongs to the persisted database row.
   // Cancelling an edit operation must not delete this file.
   private var _originalImagePath: String? = null

   // After a save request has been emitted, the coordinator owns the image-file
   // lifecycle. This prevents onCleared() from deleting a file while the
   // repository operation is still pending.
   private var _imageOwnershipTransferred = false

   init {
      AppLogger.debug(TAG,"init: isNew=$_isNew, personId=$_personId")
      if (!_isNew) loadPerson()
   }

   // The only public action entry point of this ViewModel.
   fun onIntent(intent: PersonIntent) {
      AppLogger.debug(TAG, "onIntent: ${intent.toString()}")
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

   // Loads an existing person and updates the screen state.
   private fun loadPerson() {
      val personId = _personId ?: return

      deleteUnsavedReplacementImage()

      viewModelScope.launch {
         _state.update { currentState ->
            currentState.copy(person = null, isLoading = true)
         }

         _repository.findById(personId)
            .onSuccess { person ->
               AppLogger.debug(TAG, "loadPerson: $person")
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
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = person.copy(firstName = value))
      }
   }

   private fun changeLastName(value: String) {
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = person.copy(lastName = value))
      }
   }

   private fun changeEmail(value: String) {
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = person.copy(email = value.toNullableInput()))
      }
   }

   private fun changePhone(value: String) {
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = person.copy(phone = value.toNullableInput()))
      }
   }

   private fun changeImage(value: String?) {
      AppLogger.debug(TAG, "changeImage: $value")
      val previousImagePath = _state.value.person?.imagePath
      val newImagePath = value?.takeUnless(String::isBlank)

      if (
         previousImagePath != null &&
         previousImagePath != _originalImagePath &&
         previousImagePath != newImagePath
      ) {
         deleteImageFromAppStorage(previousImagePath)
      }
      _state.update { currentState ->
         val person = currentState.person ?: return@update currentState
         currentState.copy(person = person.copy(imagePath = newImagePath))
      }
   }


   // Performs final form validation before a create or update request.
   // Field-level validation remains local to InputValueString while editing.
   private fun validateAndRequestSave() {
      AppLogger.debug(TAG, "validateAndRequestSave()")
      val person = _state.value.person?.normalized() ?: return

      _state.update { currentState ->
         currentState.copy(person = person)
      }

      val errorMessage = _validator.validatePerson(person)
      if (errorMessage != null) {
         // The validator has already resolved this text from strings.xml.
         showSnackbar(UiText.Resolved(errorMessage))
         return
      }

      _imageOwnershipTransferred = true
      viewModelScope.launch {
         _events.send(PersonEvent.RequestSave(person = person, isNew = _isNew))
      }
   }

   private fun cancelEditing() {
      AppLogger.debug(TAG, "cancelEditing()")
      deleteUnsavedReplacementImage()
      viewModelScope.launch {
         _events.send(PersonEvent.NavigateBack)
      }
   }

   private fun showSnackbar(message: UiText) {
      AppLogger.debug(TAG, "showSnackbar: $message")
      viewModelScope.launch {
         _events.send(PersonEvent.ShowSnackbar(message = message))
      }
   }

   // Reports a load failure through the shared Snackbar and then closes the
   // unusable editor entry. Both events are sent in a defined order.
   private fun showSnackbarAndNavigateBack(message: UiText) {
      AppLogger.debug(TAG, "showSnackbarAndNavigateBack: $message")
      viewModelScope.launch {
         _events.send(PersonEvent.ShowSnackbar(message = message))
         _events.send(PersonEvent.NavigateBack)
      }
   }

   private fun deleteUnsavedReplacementImage() {
      AppLogger.debug(TAG, "deleteUnsavedReplacementImage()")
      if (_imageOwnershipTransferred) return

      val currentImagePath = _state.value.person?.imagePath
      if (
         currentImagePath != null &&
         currentImagePath != _originalImagePath
      ) {
         deleteImageFromAppStorage(currentImagePath)
      }
   }

   override fun onCleared() {
      AppLogger.debug(TAG, "onCleared()")
      deleteUnsavedReplacementImage()
      super.onCleared()
   }

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}
