package de.rogallab.mobile.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualRemovalControllerTest {

   private data class Item(
      val id: String,
      val label: String,
   )

   private val first = Item(id = "1", label = "First")
   private val second = Item(id = "2", label = "Second")
   private val third = Item(id = "3", label = "Third")

   @Test
   fun removeAndRestore_keepOriginalPosition() {
      val controller = VisualRemovalController<Item>(Item::id)

      val removal = controller.remove(
         items = listOf(first, second, third),
         item = second,
         originalIndex = 1,
      )

      assertEquals(listOf(first, third), removal.items)
      assertEquals(1, removal.originalIndex)

      val restoredItems = controller.restore(
         items = removal.items,
         item = second,
         originalIndex = removal.originalIndex,
      )

      assertEquals(listOf(first, second, third), restoredItems)
   }

   @Test
   fun staleDatabaseSnapshot_keepsRemovedItemHidden() {
      val controller = VisualRemovalController<Item>(Item::id)

      controller.remove(
         items = listOf(first, second, third),
         item = second,
         originalIndex = 1,
      )

      val visibleItems = controller.visibleItems(
         listOf(third, second, first)
      )

      assertEquals(listOf(third, first), visibleItems)
   }

   @Test
   fun confirmedDeletion_clearsTemporaryRemovalId() {
      val controller = VisualRemovalController<Item>(Item::id)

      controller.remove(
         items = listOf(first, second, third),
         item = second,
         originalIndex = 1,
      )

      controller.visibleItems(listOf(first, third))

      val recreatedItemWithSameId = second.copy(label = "Recreated")
      val visibleItems = controller.visibleItems(
         listOf(first, recreatedItemWithSameId, third)
      )

      assertEquals(
         listOf(first, recreatedItemWithSameId, third),
         visibleItems,
      )
   }

   @Test
   fun missingOriginalIndex_usesCurrentItemPosition() {
      val controller = VisualRemovalController<Item>(Item::id)

      val removal = controller.remove(
         items = listOf(first, second, third),
         item = second,
         originalIndex = -1,
      )

      assertEquals(1, removal.originalIndex)
   }
}
