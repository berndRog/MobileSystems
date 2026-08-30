package de.rogallab.mobile.ui.coordinator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.common.UiMessage
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.common.uiText
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Activity-scoped coordinator for workflows shared by all three aspects.
//
// The coordinator owns one Snackbar queue and one global pending-removal slot.
// Feature ViewModels own their screen state and publish user-visible UiText
// values without depending on an Android Context.
class CoordinatorViewModel(
   private val _personRepository: IPersonRepository,
   private val _carRepository: ICarRepository,
   private val _tDriveRepository: ITDriveRepository,
) : ViewModel() {

   private sealed interface PendingRemoval {
      val originalIndex: Int
      val messageId: Long

      data class PersonRemoval(
         val person: Person,
         override val originalIndex: Int,
         override val messageId: Long,
      ) : PendingRemoval

      data class CarRemoval(
         val car: Car,
         override val originalIndex: Int,
         override val messageId: Long,
      ) : PendingRemoval

      data class TDriveRemoval(
         val tDrive: TDrive,
         override val originalIndex: Int,
         override val messageId: Long,
      ) : PendingRemoval
   }

   private val _state = MutableStateFlow(CoordinatorState())
   val state: StateFlow<CoordinatorState> = _state.asStateFlow()

   private val _personEvents =
      Channel<CoordinatorEffect.RestorePerson>(Channel.BUFFERED)
   val personEvents = _personEvents.receiveAsFlow()

   private val _carEvents =
      Channel<CoordinatorEffect.RestoreCar>(Channel.BUFFERED)
   val carEvents = _carEvents.receiveAsFlow()

   private val _tDriveEvents =
      Channel<CoordinatorEffect.RestoreTestDrive>(Channel.BUFFERED)
   val testDriveEvents = _tDriveEvents.receiveAsFlow()

   private val _waitingMessages = ArrayDeque<UiMessage>()
   private val _messageIds = AtomicLong(0)

   // A second swipe commits the previous pending removal before the newer
   // entity receives the single global Undo opportunity.
   private var _pendingRemoval: PendingRemoval? = null

   // The only public action entry point of this ViewModel.
   fun onIntent(intent: CoordinatorIntent) {
      when (intent) {
         is CoordinatorIntent.ShowMessage -> enqueueMessage(intent.text)
         is CoordinatorIntent.UndoRemove -> undoRemoval(intent.messageId)
         is CoordinatorIntent.ConfirmRemove -> confirmRemoval(intent.messageId)
         is CoordinatorIntent.MessageConsumed -> consumeMessage(intent.messageId)

         is CoordinatorIntent.SavePerson -> savePerson(intent.person, intent.isNew)
         is CoordinatorIntent.RemovePerson -> requestPersonRemoval(intent.person, intent.originalIndex)

         is CoordinatorIntent.SaveCar -> saveCar(intent.car, intent.isNew)
         is CoordinatorIntent.RemoveCar -> requestCarRemoval(intent.car, intent.originalIndex)

         is CoordinatorIntent.SaveTDrive -> saveTDrive(intent.tDrive, intent.isNew)
         is CoordinatorIntent.RemoveTDrive -> requestTDriveRemoval(intent.tDrive, intent.originalIndex)
      }
   }

   // Saves a new or updated Person to the repository and manages the associated image file.
   private fun savePerson(person: Person, isNew: Boolean) {
      viewModelScope.launch {
         setWritingState(true)

         val previousPerson =
            if (isNew) null
            else _personRepository.findById(person.id).getOrNull()

         val result =
            if (isNew) _personRepository.create(person)
            else _personRepository.update(person)

         result
            .onSuccess {
               val previousImagePath = previousPerson?.imagePath
               if (previousImagePath != person.imagePath) {
                  deleteImageFromAppStorage(previousImagePath)
               }
               enqueueMessage(
                  uiText(R.string.message_person_saved, person.displayName)
               )
            }
            .onFailure {
               val previousImagePath = previousPerson?.imagePath
               if (person.imagePath != previousImagePath) {
                  deleteImageFromAppStorage(person.imagePath)
               }
               enqueueMessage(uiText(R.string.error_person_save))
            }

         setWritingState(false)
      }
   }

   // Starts an Undo window without changing Room.
   private fun requestPersonRemoval(
      person: Person,
      originalIndex: Int,
   ) {
      commitPreviousPendingRemoval()
      val message = newMessage(
         text = uiText(R.string.message_person_removed, person.displayName),
         actionLabel = uiText(R.string.action_undo),
      )
      _pendingRemoval = PendingRemoval.PersonRemoval(person, originalIndex,message.id)
      publishMessage(message)
   }


   // Saves a new or updated Car to the repository and manages the associated image files.
   private fun saveCar(car: Car, isNew: Boolean) {
      viewModelScope.launch {
         setWritingState(true)

         val previousCar =
            if (isNew) null
            else _carRepository.findById(car.id).getOrNull()

         val result =
            if (isNew) _carRepository.create(car)
            else _carRepository.update(car)

         result
            .onSuccess {
               val currentImagePaths = car.imagePaths.toSet()
               previousCar?.imagePaths.orEmpty()
                  .filterNot { imagePath -> currentImagePaths.contains(imagePath) }
                  .forEach { imagePath ->
                     deleteImageFromAppStorage(imagePath)
                  }

               enqueueMessage(uiText(R.string.message_car_saved, car.displayName))
            }
            .onFailure {
               val previousImagePaths = previousCar?.imagePaths.orEmpty().toSet()
               car.imagePaths
                  .filterNot { imagePath -> previousImagePaths.contains(imagePath) }
                  .forEach { imagePath ->
                     deleteImageFromAppStorage(imagePath)
                  }

               enqueueMessage(uiText(R.string.error_car_save))
            }

         setWritingState(false)
      }
   }

   // Starts an Undo window without changing Room.
   private fun requestCarRemoval(
      car: Car,
      originalIndex: Int,
   ) {
      commitPreviousPendingRemoval()
      val message = newMessage(
         text = uiText(R.string.message_car_removed, car.displayName),
         actionLabel = uiText(R.string.action_undo),
      )
      _pendingRemoval = PendingRemoval.CarRemoval(car,  originalIndex, message.id)
      publishMessage(message)
   }

   private fun saveTDrive(
      tDrive: TDrive,
      isNew: Boolean,
   ) {
      viewModelScope.launch {
         setWritingState(true)
         val result =
            if (isNew) _tDriveRepository.create(tDrive)
            else _tDriveRepository.update(tDrive)

         result
            .onSuccess {
               enqueueMessage(uiText(R.string.message_test_drive_saved))
            }
            .onFailure {
               enqueueMessage(uiText(R.string.error_test_drive_save))
            }
         setWritingState(false)
      }
   }

   // Starts an Undo window without changing Room.
   private fun requestTDriveRemoval(
      tDrive: TDrive,
      originalIndex: Int,
   ) {
      commitPreviousPendingRemoval()
      val message = newMessage(
         text = uiText(R.string.message_test_drive_removed),
         actionLabel = uiText(R.string.action_undo),
      )
      _pendingRemoval = PendingRemoval.TDriveRemoval(
         tDrive, originalIndex, message.id)
      publishMessage(message)
   }

   private fun commitPreviousPendingRemoval() {
      val previousRemoval = _pendingRemoval ?: return
      _pendingRemoval = null
      discardMessage(previousRemoval.messageId)
      persistRemoval(previousRemoval)
   }

   // Undo restores only the visual list item because Room was not changed yet.
   private fun undoRemoval(messageId: Long) {
      val pendingRemoval = _pendingRemoval
         ?.takeIf { removal -> removal.messageId == messageId }
         ?: return

      _pendingRemoval = null
      restoreVisualItem(pendingRemoval)
   }

   // A dismissed Undo Snackbar commits the final repository deletion.
   private fun confirmRemoval(messageId: Long) {
      val pendingRemoval = _pendingRemoval
         ?.takeIf { removal -> removal.messageId == messageId }
         ?: return

      _pendingRemoval = null
      persistRemoval(pendingRemoval)
   }

   // Commits the final repository deletion and manages the associated image files.
   private fun persistRemoval(pendingRemoval: PendingRemoval) {
      viewModelScope.launch {
         setWritingState(true)

         when (pendingRemoval) {
            is PendingRemoval.PersonRemoval -> {
               removePerson(pendingRemoval.person)
                  .onFailure {
                     restoreVisualItem(pendingRemoval)
                     enqueueMessage(uiText(R.string.error_person_delete))
                  }
            }

            is PendingRemoval.CarRemoval -> {
               removeCar(pendingRemoval.car)
                  .onFailure {
                     restoreVisualItem(pendingRemoval)
                     enqueueMessage(uiText(R.string.error_car_delete))
                  }
            }

            is PendingRemoval.TDriveRemoval -> {
               _tDriveRepository.remove(pendingRemoval.tDrive)
                  .onFailure {
                     restoreVisualItem(pendingRemoval)
                     enqueueMessage(uiText(R.string.error_test_drive_delete))
                  }
            }
         }

         setWritingState(false)
      }
   }

   // Deletes the Person from the repository and manages the associated image file.
   private suspend fun removePerson(person: Person): Result<Unit> {
      val persistedPerson = _personRepository.findById(person.id).getOrNull()
      return _personRepository.remove(person)
         .onSuccess {
            deleteImageFromAppStorage(persistedPerson?.imagePath)
            if (person.imagePath != persistedPerson?.imagePath) {
               deleteImageFromAppStorage(person.imagePath)
            }
         }
         .onFailure {
            if (person.imagePath != persistedPerson?.imagePath) {
               deleteImageFromAppStorage(person.imagePath)
            }
         }
   }

   // Deletes the Car from the repository and manages the associated image files.
   private suspend fun removeCar(car: Car): Result<Unit> {
      val persistedCar = _carRepository.findById(car.id).getOrNull()
      return _carRepository.remove(car)
         .onSuccess {
            (persistedCar?.imagePaths ?: car.imagePaths)
               .forEach { imagePath ->
                  deleteImageFromAppStorage(imagePath)
               }
         }
   }

   // Restores the visual list item without changing Room.
   private fun restoreVisualItem(pendingRemoval: PendingRemoval) {
      viewModelScope.launch {
         when (pendingRemoval) {
            is PendingRemoval.PersonRemoval -> _personEvents.send(
               CoordinatorEffect.RestorePerson(
                  person = pendingRemoval.person,
                  originalIndex = pendingRemoval.originalIndex,
               )
            )

            is PendingRemoval.CarRemoval -> _carEvents.send(
               CoordinatorEffect.RestoreCar(
                  car = pendingRemoval.car,
                  originalIndex = pendingRemoval.originalIndex,
               )
            )

            is PendingRemoval.TDriveRemoval -> _tDriveEvents.send(
               CoordinatorEffect.RestoreTestDrive(
                  tDrive = pendingRemoval.tDrive,
                  originalIndex = pendingRemoval.originalIndex,
               )
            )
         }
      }
   }

   private fun setWritingState(isWriting: Boolean) {
      _state.update { coordinatorState ->
         coordinatorState.copy(isWriting = isWriting)
      }
   }

   private fun newMessage(
      text: UiText,
      actionLabel: UiText? = null,
   ): UiMessage = UiMessage(
      id = _messageIds.incrementAndGet(),
      text = text,
      actionLabel = actionLabel,
   )

   private fun enqueueMessage(
      text: UiText,
      actionLabel: UiText? = null,
   ) {
      publishMessage(newMessage(text, actionLabel))
   }

   private fun publishMessage(message: UiMessage) {
      if (_state.value.message == null) {
         _state.update { coordinatorState ->
            coordinatorState.copy(message = message)
         }
      } else {
         _waitingMessages.addLast(message)
      }
   }

   private fun discardMessage(messageId: Long) {
      if (_state.value.message?.id == messageId) {
         val nextMessage =
            if (_waitingMessages.isEmpty()) null
            else _waitingMessages.removeFirst()
         _state.update { coordinatorState ->
            coordinatorState.copy(message = nextMessage)
         }
      } else {
         _waitingMessages.removeIf { message -> message.id == messageId }
      }
   }

   private fun consumeMessage(messageId: Long) {
      if (_state.value.message?.id != messageId) return
      val nextMessage =
         if (_waitingMessages.isEmpty()) null
         else _waitingMessages.removeFirst()
      _state.update { coordinatorState ->
         coordinatorState.copy(message = nextMessage)
      }
   }
}