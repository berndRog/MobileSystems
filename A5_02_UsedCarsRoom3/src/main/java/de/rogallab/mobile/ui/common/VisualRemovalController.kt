package de.rogallab.mobile.ui.common

/**
 * Manages the temporary visual removal of list items until the repository
 * confirms the deletion or the user restores the item with Undo.
 *
 * The controller is independent of ViewModel, UiState, Intent and Event types.
 * A list ViewModel delegates only the technical list operations to it.
 */
class VisualRemovalController<T>(
   private val _idOf: (T) -> String,
) {

   private val _visuallyRemovedIds = mutableSetOf<String>()

   /**
    * Filters a new database snapshot without immediately showing items that
    * are still waiting for the persistent delete operation.
    *
    * IDs that are no longer part of the database snapshot are removed from
    * the internal buffer because their deletion has been confirmed.
    */
   fun visibleItems(databaseItems: List<T>): List<T> {
      val databaseIds = databaseItems.mapTo(mutableSetOf(), _idOf)
      _visuallyRemovedIds.removeAll { itemId -> itemId !in databaseIds }

      return databaseItems.filterNot { item ->
         _idOf(item) in _visuallyRemovedIds
      }
   }

   /**
    * Removes an item from the visible list and remembers its ID.
    *
    * The returned index is stable even when the caller did not provide a
    * valid original index.
    */
   fun remove(
      items: List<T>,
      item: T,
      originalIndex: Int,
   ): VisualRemovalResult<T> {
      val itemId = _idOf(item)
      val actualIndex = items.indexOfFirst { currentItem ->
         _idOf(currentItem) == itemId
      }
      val stableIndex = when {
         originalIndex >= 0 -> originalIndex
         actualIndex >= 0 -> actualIndex
         else -> 0
      }

      _visuallyRemovedIds.add(itemId)

      return VisualRemovalResult(
         items = items.filterNot { currentItem ->
            _idOf(currentItem) == itemId
         },
         originalIndex = stableIndex,
      )
   }

   /**
    * Restores an item at its previous position and removes its ID from the
    * temporary visual-removal buffer.
    */
   fun restore(
      items: List<T>,
      item: T,
      originalIndex: Int,
   ): List<T> {
      val itemId = _idOf(item)
      _visuallyRemovedIds.remove(itemId)

      return items
         .filterNot { currentItem -> _idOf(currentItem) == itemId }
         .toMutableList()
         .apply {
            add(
               index = originalIndex.coerceIn(0, size),
               element = item,
            )
         }
   }
}

data class VisualRemovalResult<T>(
   val items: List<T>,
   val originalIndex: Int,
)
