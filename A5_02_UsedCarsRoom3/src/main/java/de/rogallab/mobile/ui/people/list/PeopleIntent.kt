package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleIntent {
   data object Create : PeopleIntent
   data class Open(val personId: String) : PeopleIntent
   data class Remove(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleIntent
   data class Restore(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleIntent
   data object Restored : PeopleIntent
}
