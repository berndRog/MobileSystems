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
 *   Swipe-to-Detail erzeugt Navigation; Swipe-to-Delete fordert zunächst eine
 *   Bestätigung über einen einmaligen Effect an.
 *
 * - Erst nach der Bestätigung wird das Repository geändert. Die aktualisierte
 *   Liste kommt anschließend wieder über observeAll() in den State.
 *
 * - Die offene Bestätigungs-Snackbar wird bewusst nicht als dauerhafter State
 *   modelliert. Ein temporärer Removal-State für Undo folgt erst in A3_05.
 *
 * Lernziele:
 *
 * - Gesten als Events verstehen, ohne dafür unnötigen State einzuführen.
 * - Bestätigungsdialog und dauerhaften UI-State voneinander unterscheiden.
 * - Persistierten Repository-State weiterhin als Quelle der Listenanzeige nutzen.
 */
