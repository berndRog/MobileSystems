package de.rogallab.mobile.ui.people.create_detail

import com.google.errorprone.annotations.Immutable
import de.rogallab.mobile.domain.entities.Person

@Immutable
data class PersonUiState(
   val person: Person = Person(),
   val isNew: Boolean = true,
   val isLoading: Boolean = false,
   val isSaving: Boolean = false,
   val loadFailure: PersonLoadFailure? = null,
)


/*
 * Didaktik und Lernziele
 *
 * - isSaving zeigt einen laufenden Speichervorgang an.
 * - loadFailure unterscheidet eine fehlende Person von einem Repository-Fehler.
 *   Beide bleiben sichtbar, bis ein sinnvoller weiterer Schritt möglich ist.
 */
