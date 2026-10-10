package de.rogallab.mobile.ui.people.create_detail

import de.rogallab.mobile.domain.entities.Person

data class PersonUiState(
   val person: Person = Person(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
)

/*
 * Didaktik und Lernziele
 *
 * - isSaving beschreibt einen laufenden Speichervorgang als sichtbaren UI-State.
 *   Adapter und Navigation können damit dieselbe Entscheidung verwenden.
 */
