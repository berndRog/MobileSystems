package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.ui.components.SwipeCard
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
}
