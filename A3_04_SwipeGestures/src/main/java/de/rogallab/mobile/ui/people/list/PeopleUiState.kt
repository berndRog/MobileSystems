package de.rogallab.mobile.ui.people.list

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.Person

@Immutable
data class PeopleUiState(
   val isLoading: Boolean = false,
   val people: List<Person> = emptyList(),
   val restoredPersonId: String? = null,
)

/*
 * Didaktik und Lernziele
 *
 * - people enthält weiterhin die aktuell sichtbare Liste der Personen.
 *
 * - restoredPersonId ist ein kurzlebiger UI-State-Trigger. Nach Undo enthält er
 *   die ID der wieder eingeblendeten Person, damit die LazyColumn sicherstellen
 *   kann, dass dieses Element auch im sichtbaren Viewport liegt.
 *
 * - Nachdem die UI den Scroll-Auftrag verarbeitet hat, wird restoredPersonId
 *   über einen Intent wieder auf null gesetzt.
 *
 * Lernziele:
 *
 * - Dauerhaften Screen-State und kurzlebige UI-Aufträge im selben State klar
 *   voneinander unterscheiden.
 * - Einen UI-Auftrag nach seiner Verarbeitung explizit bestätigen.
 */
