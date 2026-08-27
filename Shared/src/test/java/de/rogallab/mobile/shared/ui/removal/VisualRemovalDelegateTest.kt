package de.rogallab.mobile.shared.ui.removal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualRemovalDelegateTest {

   private data class Item(
      val id: String,
      val name: String,
   )

   private val ada = Item("p1", "Ada")
   private val grace = Item("p2", "Grace")

   private fun createDelegate() =
      VisualRemovalDelegate<Item> { item -> item.id }

   @Test
   fun remove_hidesItemAndKeepsPendingItem() {
      val delegate = createDelegate()
      delegate.update(listOf(ada, grace))

      assertTrue(delegate.remove(ada))

      assertEquals(listOf(grace), delegate.visibleItems())
      assertEquals(ada, delegate.pending("p1"))
   }

   @Test
   fun removeSameItemTwice_isIgnored() {
      val delegate = createDelegate()
      delegate.update(listOf(ada, grace))

      assertTrue(delegate.remove(ada))
      assertFalse(delegate.remove(ada))

      assertEquals(listOf(grace), delegate.visibleItems())
   }

   @Test
   fun undo_restoresItemWithoutChangingSourceList() {
      val delegate = createDelegate()
      delegate.update(listOf(ada, grace))
      delegate.remove(ada)

      assertTrue(delegate.undo("p1"))

      assertEquals(listOf(ada, grace), delegate.visibleItems())
      assertNull(delegate.pending("p1"))
   }

   @Test
   fun commit_keepsItemHiddenUntilSourceConfirmsRemoval() {
      val delegate = createDelegate()
      delegate.update(listOf(ada, grace))
      delegate.remove(ada)

      delegate.commit("p1")
      assertEquals(listOf(grace), delegate.visibleItems())
      assertNull(delegate.pending("p1"))

      delegate.update(listOf(grace))
      assertEquals(listOf(grace), delegate.visibleItems())
   }

   @Test
   fun restore_makesItemVisibleAfterFailedPersistence() {
      val delegate = createDelegate()
      delegate.update(listOf(ada, grace))
      delegate.remove(ada)

      delegate.restore("p1")

      assertEquals(listOf(ada, grace), delegate.visibleItems())
      assertNull(delegate.pending("p1"))
   }
}
