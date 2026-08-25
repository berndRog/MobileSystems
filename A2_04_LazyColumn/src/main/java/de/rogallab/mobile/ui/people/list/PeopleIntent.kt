package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleIntent {
   data class OpenDetail(val person: Person) : PeopleIntent
   data class Remove(val person: Person) : PeopleIntent
}