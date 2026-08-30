package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.common.UiText

sealed interface PeopleEfect {
   data object NavigateToCreate : PeopleEfect
   data class NavigateToDetails(val personId: String) : PeopleEfect
   data class RequestRemove(
      val person: Person,
      val originalIndex: Int,
   ) : PeopleEfect
   data class ShowSnackbar(val message: UiText) : PeopleEfect
}
