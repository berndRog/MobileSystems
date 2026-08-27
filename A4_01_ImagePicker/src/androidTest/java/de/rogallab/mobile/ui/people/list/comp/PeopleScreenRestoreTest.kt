package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PeopleScreenRestoreTest {

   @get:Rule
   val composeRule = createComposeRule()

   @Test
   fun restoredPerson_scrollsIntoViewAndAcknowledges() {
      val people = (0..9).map { index ->
         Person(
            firstName = "Person",
            lastName = index.toString(),
            id = "p$index",
         )
      }
      val restoredPersonId = mutableStateOf<String?>(null)
      var handledCount = 0

      composeRule.setContent {
         MaterialTheme {
            PeopleScreen(
               people = people,
               restoredPersonId = restoredPersonId.value,
               onRestoreHandled = {
                  handledCount++
                  restoredPersonId.value = null
               },
               onDetail = {},
               onEdit = {},
               onDelete = {},
               modifier = Modifier
                  .height(180.dp)
                  .testTag("peopleList"),
            )
         }
      }

      // Move the viewport away from the first item.
      composeRule.onNodeWithTag("peopleList")
         .performScrollToNode(hasText("Person 9"))
      composeRule.waitForIdle()

      // Simulates Undo of the first list item.
      composeRule.runOnIdle {
         restoredPersonId.value = "p0"
      }

      composeRule.waitUntil(timeoutMillis = 5_000) {
         handledCount == 1
      }

      composeRule.onNodeWithText("Person 0")
         .assertIsDisplayed()
      assertEquals(1, handledCount)
   }
}
