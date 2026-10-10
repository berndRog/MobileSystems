package de.rogallab.mobile.ui.people.list

import de.rogallab.mobile.domain.entities.Person

sealed interface PeopleIntent {
   data object Create : PeopleIntent
   data class Detail(val personId: String) : PeopleIntent
   data class Remove(val person: Person) : PeopleIntent
   data object RetryLoad : PeopleIntent
}

/*
 * Didaktik und Lernziele
 *
 * - RetryLoad startet einen neuen Ladeversuch nur nach einer Benutzeraktion.
 */
