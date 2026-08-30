package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.domain.entities.TDrive
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.entities.Car

sealed interface CoordinatorEffect {
   data class RestorePerson(
      val person: Person,
      val originalIndex: Int,
   ) : CoordinatorEffect

   data class RestoreCar(
      val car: Car,
      val originalIndex: Int,
   ) : CoordinatorEffect

   data class RestoreTestDrive(
      val tDrive: TDrive,
      val originalIndex: Int,
   ) : CoordinatorEffect
}
