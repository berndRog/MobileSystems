package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PeopleScreenTest {
   @get:Rule val composeRule = createComposeRule()

   @Test fun personCardShowsNameAndForwardsDetailAndRemove() {
      val person = Person(firstName = "Ada", lastName = "Lovelace", id = "person-1")
      var detailed: Person? = null
      var removed: Person? = null
      composeRule.setContent {
         PeopleScreen(
            people = listOf(person),
            onDetail = { detailed = it },
            onRemove = { removed = it },
         )
      }

      composeRule.onNodeWithText("Ada Lovelace").assertExists().performClick()
      composeRule.runOnIdle { assertEquals(person, detailed) }
      composeRule.onNodeWithContentDescription("Card löschen").performClick()
      composeRule.runOnIdle { assertEquals(person, removed) }
   }
}
