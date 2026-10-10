package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person = Person(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
   val loadFailure: PersonLoadFailure? = null,
)

sealed interface PersonLoadFailure {
   val message: String

   data class NotFound(override val message: String) : PersonLoadFailure
   data class Failed(override val message: String) : PersonLoadFailure
}

/*
 * Didaktik und Lernziele
 *
 * - isSaving beschreibt einen laufenden Speichervorgang als sichtbaren UI-State.
 *   Adapter und Navigation können damit dieselbe Entscheidung verwenden.
 * - loadFailure hält einen Ladefehler sichtbar, bis ein neuer Ladeversuch beginnt
 *   oder die Person erfolgreich geladen wurde.
 */
