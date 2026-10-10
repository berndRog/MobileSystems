package de.rogallab.mobile.ui.people.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Person

@Immutable
data class PeopleUiState(
   val isLoading: Boolean = false,
   val people: List<Person> = emptyList(),
   val loadFailure: String? = null,
)

/*
 * Didaktik und Lernziele
 *
 * - Ein Ladefehler bleibt als Zustand sichtbar, bis ein neuer Versuch beginnt.
 * - Eine erfolgreich geladene leere Liste ist ein gültiger Datenbestand.
 */
