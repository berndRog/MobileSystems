package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.common.UiText

sealed interface CoordinatorIntent {
   data class ShowMessage(val text: UiText, ) : CoordinatorIntent

   data class UndoRemove(
      val messageId: Long,
   ) : CoordinatorIntent

   data class ConfirmRemove(
      val messageId: Long,
   ) : CoordinatorIntent

   data class MessageConsumed(
      val messageId: Long,
   ) : CoordinatorIntent

   data class SavePerson(
      val person: Person,
      val isNew: Boolean,
   ) : CoordinatorIntent

   data class SaveCar(
      val car: Car,
      val isNew: Boolean,
   ) : CoordinatorIntent

   data class SaveTDrive(
      val tDrive: TDrive,
      val isNew: Boolean,
   ) : CoordinatorIntent

   data class RemovePerson(
      val person: Person,
      val originalIndex: Int,
   ) : CoordinatorIntent

   data class RemoveCar(
      val car: Car,
      val originalIndex: Int,
   ) : CoordinatorIntent

   data class RemoveTDrive(
      val tDrive: TDrive,
      val originalIndex: Int,
   ) : CoordinatorIntent


}
