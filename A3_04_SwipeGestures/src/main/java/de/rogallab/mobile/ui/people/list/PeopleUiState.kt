package de.rogallab.mobile.ui.people.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Person

@Immutable
data class PeopleUiState(
   val isLoading: Boolean = false,
   val people: List<Person> = emptyList(),
)

/*
 * Didaktik und Lernziele
 *
 * - PeopleUiState enthält weiterhin ausschließlich den beobachtbaren Zustand
 *   der Personenliste.
 *
 * - Swipe-Gesten benötigen in diesem Schritt keinen zusätzlichen Screen-State.
 *   Swipe-to-Edit erzeugt Navigation; Swipe-to-Delete führt unmittelbar zu
 *   einer Repository-Operation. Die aktualisierte Liste kommt anschließend
 *   wieder über observeAll() in den State.
 *
 * - Ein temporärer Zustand für Undo wird bewusst erst in A3_05 eingeführt.
 *
 * Lernziele:
 *
 * - Gesten als Events verstehen, ohne dafür unnötigen State einzuführen.
 * - Persistierten Repository-State weiterhin als Quelle der Listenanzeige nutzen.
 */
