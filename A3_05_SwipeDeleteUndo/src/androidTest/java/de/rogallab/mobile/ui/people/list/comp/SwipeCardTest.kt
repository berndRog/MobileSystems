package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.ui.components.SwipeCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SwipeCardTest {

   @get:Rule
   val composeRule = createComposeRule()

   @Test
   fun swipeStartToEnd_callsDetail() {
      var detailed = false

      composeRule.setContent {
         MaterialTheme {
            SwipeCard(
               onDetail = { detailed = true },
               onDelete = {},
               detailContentDescription = "Detail",
               deleteContentDescription = "Delete",
               modifier = Modifier
                  .fillMaxWidth()
                  .height(80.dp)
                  .testTag("personSwipe"),
            ) {
               Box(modifier = Modifier.fillMaxSize())
            }
         }
      }

      composeRule.onNodeWithTag("personSwipe")
         .performTouchInput { swipeRight() }
      composeRule.waitForIdle()

      assertTrue(detailed)
   }

   @Test
   fun swipeEndToStart_callsDelete() {
      var deleted = false

      composeRule.setContent {
         MaterialTheme {
            SwipeCard(
               onDetail = {},
               onDelete = { deleted = true },
               detailContentDescription = "Detail",
               deleteContentDescription = "Delete",
               modifier = Modifier
                  .fillMaxWidth()
                  .height(80.dp)
                  .testTag("personSwipe"),
            ) {
               Box(modifier = Modifier.fillMaxSize())
            }
         }
      }

      composeRule.onNodeWithTag("personSwipe")
         .performTouchInput { swipeLeft() }
      composeRule.waitForIdle()

      assertTrue(deleted)
   }

   @Test
   fun restoredAnimatedItem_doesNotDeleteAgainWithoutNewSwipe() {
      val visibleIds = mutableStateOf(listOf("p1", "p2"))
      var deleteCount = 0

      composeRule.setContent {
         MaterialTheme {
            LazyColumn {
               items(
                  items = visibleIds.value,
                  key = { id -> id },
               ) { id ->
                  SwipeCard(
                     onDetail = {},
                     onDelete = {
                        deleteCount++
                        visibleIds.value = visibleIds.value.filterNot { it == id }
                     },
                     detailContentDescription = "Detail",
                     deleteContentDescription = "Delete",
                     modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("personSwipe_$id"),
                  ) {
                     Box(modifier = Modifier.fillMaxSize())
                  }
               }
            }
         }
      }

      composeRule.onNodeWithTag("personSwipe_p1")
         .performTouchInput { swipeLeft() }
      composeRule.waitForIdle()
      assertEquals(1, deleteCount)

      // Simulates Undo while LazyColumn uses the same stable key and item animation.
      composeRule.runOnIdle {
         visibleIds.value = listOf("p1", "p2")
      }
      composeRule.waitForIdle()

      // Restoring the item must not replay the previous delete gesture.
      assertEquals(1, deleteCount)

      // A new explicit swipe may delete the restored item again.
      composeRule.onNodeWithTag("personSwipe_p1")
         .performTouchInput { swipeLeft() }
      composeRule.waitForIdle()
      assertEquals(2, deleteCount)
   }
}
