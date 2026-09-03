package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

   private val allPeople = (0..9).map { index ->
      Person(
         firstName = "Person",
         lastName = index.toString(),
         id = "p$index",
      )
   }

   @Test
   fun restoredFirstPerson_afterRealRemoval_isVisibleAndAcknowledged() {
      val people = mutableStateOf(allPeople)
      val restoredPersonId = mutableStateOf<String?>(null)
      var handledCount = 0

      composeRule.setContent {
         MaterialTheme {
            PeopleScreen(
               people = people.value,
               restoredPersonId = restoredPersonId.value,
               onRestoreHandled = {
                  handledCount++
                  restoredPersonId.value = null
               },
               onDetail = {},
               onDelete = {},
               modifier = Modifier
                  .height(180.dp)
                  .testTag("peopleList"),
            )
         }
      }

      // Simulates visual removal of the first item. Person 1 now occupies index 0.
      composeRule.runOnIdle {
         people.value = allPeople.drop(1)
      }
      composeRule.waitForIdle()
      assertEquals(
         0,
         composeRule.onAllNodesWithText("Person 0")
            .fetchSemanticsNodes().size,
      )

      // Simulates Undo: the same stable key is inserted again at index 0.
      composeRule.runOnIdle {
         people.value = allPeople
         restoredPersonId.value = "p0"
      }

      composeRule.waitUntil(timeoutMillis = 5_000) {
         handledCount == 1
      }

      composeRule.onNodeWithText("Person 0").assertIsDisplayed()
      assertEquals(1, handledCount)
   }

   @Test
   fun restoredLastPerson_afterRealRemoval_isVisibleAndAcknowledged() {
      val people = mutableStateOf(allPeople)
      val restoredPersonId = mutableStateOf<String?>(null)
      var handledCount = 0

      composeRule.setContent {
         MaterialTheme {
            PeopleScreen(
               people = people.value,
               restoredPersonId = restoredPersonId.value,
               onRestoreHandled = {
                  handledCount++
                  restoredPersonId.value = null
               },
               onDetail = {},
               onDelete = {},
               modifier = Modifier
                  .height(180.dp)
                  .testTag("peopleList"),
            )
         }
      }

      // Put the viewport at the lower edge before removing the last item.
      composeRule.onNodeWithTag("peopleList")
         .performScrollToNode(hasText("Person 9"))
      composeRule.waitForIdle()

      composeRule.runOnIdle {
         people.value = allPeople.dropLast(1)
      }
      composeRule.waitForIdle()
      assertEquals(
         0,
         composeRule.onAllNodesWithText("Person 9")
            .fetchSemanticsNodes().size,
      )

      // Undo reinserts the item below the current lower edge.
      composeRule.runOnIdle {
         people.value = allPeople
         restoredPersonId.value = "p9"
      }

      composeRule.waitUntil(timeoutMillis = 5_000) {
         handledCount == 1
      }

      composeRule.onNodeWithText("Person 9").assertIsDisplayed()
      assertEquals(1, handledCount)
   }
}
