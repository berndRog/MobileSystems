package de.rogallab.mobile.ui.people.list.comp

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SwipePersonCardTest {

   @get:Rule
   val composeRule = createComposeRule()

   @Test
   fun swipeStartToEnd_callsEdit() {
      var edited = false

      composeRule.setContent {
         MaterialTheme {
            SwipePersonCard(
               firstName = "Ada",
               lastName = "Lovelace",
               email = null,
               phone = null,
               imagePath = null,
               onDetail = {},
               onEdit = { edited = true },
               onDelete = {},
               modifier = Modifier
                  .fillMaxWidth()
                  .height(80.dp)
                  .testTag("personSwipe"),
            )
         }
      }

      composeRule.onNodeWithTag("personSwipe")
         .performTouchInput { swipeRight() }
      composeRule.waitForIdle()

      assertTrue(edited)
   }

   @Test
   fun swipeEndToStart_callsDelete() {
      var deleted = false

      composeRule.setContent {
         MaterialTheme {
            SwipePersonCard(
               firstName = "Ada",
               lastName = "Lovelace",
               email = null,
               phone = null,
               imagePath = null,
               onDetail = {},
               onEdit = {},
               onDelete = { deleted = true },
               modifier = Modifier
                  .fillMaxWidth()
                  .height(80.dp)
                  .testTag("personSwipe"),
            )
         }
      }

      composeRule.onNodeWithTag("personSwipe")
         .performTouchInput { swipeLeft() }
      composeRule.waitForIdle()

      assertTrue(deleted)
   }

   @Test
   fun restoredItem_doesNotDeleteAgainWithoutNewSwipe() {
      val visibleIds = mutableStateOf(listOf("p1"))
      var deleteCount = 0

      composeRule.setContent {
         MaterialTheme {
            LazyColumn {
               items(
                  items = visibleIds.value,
                  key = { id -> id },
               ) { id ->
                  SwipePersonCard(
                     firstName = "Ada",
                     lastName = "Lovelace",
                     email = null,
                     phone = null,
                     imagePath = null,
                     onDetail = {},
                     onEdit = {},
                     onDelete = {
                        deleteCount++
                        visibleIds.value = emptyList()
                     },
                     modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("personSwipe_$id"),
                  )
               }
            }
         }
      }

      composeRule.onNodeWithTag("personSwipe_p1")
         .performTouchInput { swipeLeft() }
      composeRule.waitForIdle()
      assertEquals(1, deleteCount)

      // Simulates Undo: the same stable item key returns to the LazyColumn.
      composeRule.runOnIdle {
         visibleIds.value = listOf("p1")
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
