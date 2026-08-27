package de.rogallab.mobile.shared.ui.removal

/**
 * Manages items that are temporarily hidden from a visible list while an
 * Undo operation is still possible.
 *
 * The interface is independent of a concrete entity, repository or UI toolkit.
 * It only describes the temporary state between visual removal, Undo and the
 * final persistence result.
 */
interface IVisualRemoval<T> {

   // Replaces the current persistent source list.
   fun update(items: List<T>)

   // Hides one item temporarily and remembers it for a possible later commit.
   // Returns false when the same item is already pending.
   fun remove(item: T): Boolean

   // Cancels a pending removal and makes the item visible again.
   // Returns false when no pending removal exists for the id.
   fun undo(id: String): Boolean

   // Returns the item that still waits for the final persistence operation.
   fun pending(id: String): T?

   // Marks a successful persistence operation as completed. The temporary
   // hidden state ends once the source list has confirmed the removal.
   fun commit(id: String)

   // Restores an item after the persistence operation failed.
   fun restore(id: String)

   // Returns the persistent source list without temporarily hidden items.
   fun visibleItems(): List<T>
}

/*
 * Didaktik und Lernziele
 *
 * - IVisualRemoval beschreibt nur die Operationen für einen vorübergehenden
 *   visuellen Löschzustand. Die Schnittstelle kennt weder Person noch Room,
 *   Snackbar, Compose oder Coroutines.
 *
 * - Das ViewModel implementiert diese Schnittstelle bewusst nicht selbst.
 *   Stattdessen erhält es ein IVisualRemoval<T> als Abhängigkeit und ruft die
 *   benötigten Operationen explizit auf. Damit wird eine Aufgabe durch
 *   Komposition an ein spezialisiertes Objekt delegiert.
 *
 * - Diese Form unterscheidet sich von der Kotlin Interface Delegation mit
 *   "by", wie sie beim EffectDelegate verwendet wird. Das ViewModel wird also
 *   nicht selbst zu einem IVisualRemoval<T>.
 *
 * Lernziele:
 *
 * - Interface Delegation mit "by" von Delegation durch Komposition abgrenzen.
 * - Eine klar umrissene Verantwortung über ein kleines Interface kapseln.
 * - UI-, Persistenz- und temporären Bearbeitungszustand getrennt halten.
 */
