package de.rogallab.mobile.shared.ui.removal

/**
 * Stores the temporary removal state for one list feature.
 *
 * The delegate keeps the current source list, the ids hidden only in the UI
 * and the items that still wait for a final persistence decision. It does not
 * access a repository and does not emit UI effects.
 */
class VisualRemovalDelegate<T>(
   private val idOf: (T) -> String,
) : IVisualRemoval<T> {

   // Holds the latest persistent source list received from the ViewModel.
   private var _items: List<T> = emptyList()

   // IDs hidden only from the visible UI list.
   private val _hiddenIds = mutableSetOf<String>()

   // Items that may still be restored by Undo or later persisted by the ViewModel.
   private val _pendingItems = mutableMapOf<String, T>()

   // Updates the persistent source list and releases hidden ids once the source
   // confirms that a committed item no longer exists.
   override fun update(items: List<T>) {
      _items = items

      val itemIds = items.mapTo(mutableSetOf(), idOf)

      _hiddenIds.removeAll { id ->
         id !in _pendingItems && id !in itemIds
      }
   }

   // Starts one temporary removal. Repeated removal requests for the same item
   // are ignored so that only one Undo operation is created.
   override fun remove(item: T): Boolean {
      val id = idOf(item)
      if (id in _pendingItems) return false

      _pendingItems[id] = item
      _hiddenIds += id
      return true
   }

   // Cancels a pending removal and makes the item visible again immediately.
   override fun undo(id: String): Boolean {
      if (_pendingItems.remove(id) == null) return false

      _hiddenIds.remove(id)
      return true
   }

   // Returns the original item needed for the later repository operation.
   override fun pending(id: String): T? =
      _pendingItems[id]

   // Ends the Undo phase after persistence succeeded. The id intentionally
   // remains hidden until update(...) confirms the repository change.
   override fun commit(id: String) {
      _pendingItems.remove(id)
   }

   // Restores the visual state after persistence failed.
   override fun restore(id: String) {
      _pendingItems.remove(id)
      _hiddenIds.remove(id)
   }

   // Derives the visible list from the latest source data and temporary filter.
   override fun visibleItems(): List<T> =
      _items.filterNot { item ->
         idOf(item) in _hiddenIds
      }
}

/*
 * Didaktik und Lernziele
 *
 * - VisualRemovalDelegate kapselt ausschließlich den temporären Zustand einer
 *   visuellen Entfernung. Er kennt weder das Repository noch PeopleUiState,
 *   PeopleEffect, SnackbarController oder Compose.
 *
 * - _items enthält den zuletzt bekannten persistenten Ausgangszustand.
 *   _hiddenIds beschreibt davon abweichend nur, welche Einträge aktuell in der
 *   Oberfläche verborgen bleiben. _pendingItems hält zusätzlich die Objekte,
 *   die bei Ablauf des Undo-Fensters noch persistiert gelöscht werden können.
 *
 * - remove(...) verändert deshalb nur den temporären Zustand. undo(...) kann
 *   diesen Zustand vollständig zurücknehmen, ohne eine Repository-Operation
 *   rückgängig machen zu müssen.
 *
 * - Nach erfolgreicher Persistenz entfernt commit(...) nur den Pending-Eintrag.
 *   Die id bleibt zunächst verborgen. Erst update(...) löst den visuellen
 *   Filter, sobald das Repository bestätigt, dass das Objekt nicht mehr
 *   vorhanden ist.
 *
 * - Nach einem Persistenzfehler entfernt restore(...) dagegen sowohl Pending-
 *   als auch Hidden-Zustand. Das ViewModel kann die sichtbare Liste anschließend
 *   neu veröffentlichen und zusätzlich einen ShowError-Effect erzeugen.
 *
 * - Der Delegate besitzt bewusst keinen StateFlow und keine Coroutines. Der
 *   beobachtbare Screen-State bleibt vollständig im jeweiligen ViewModel.
 *
 * Lernziele:
 *
 * - Temporären UI-Zustand in einer wiederverwendbaren Komponente kapseln.
 * - Delegation durch Komposition statt ViewModel-Vererbung einsetzen.
 * - Persistenzverantwortung weiterhin im ViewModel bzw. Repository belassen.
 * - Undo implementieren, bevor eine destruktive Persistenzoperation erfolgt.
 */
