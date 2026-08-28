package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
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
}
