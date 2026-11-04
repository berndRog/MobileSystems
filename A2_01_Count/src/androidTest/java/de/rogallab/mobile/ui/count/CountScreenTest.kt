package de.rogallab.mobile.ui.count

import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.rogallab.mobile.ui.count.composables.CountScreen3
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CountScreenTest {
   @get:Rule val composeRule = createComposeRule()

   @Test fun buttonReportsIncrementAndShowsUpdatedCount() {
      val count = mutableIntStateOf(0)
      composeRule.setContent {
         CountScreen3(count = count.intValue, onIncrementCount = { count.intValue++ }, modifier = Modifier)
      }

      composeRule.onNodeWithText("0").assertExists()
      composeRule.onNodeWithText("Hochzählen").performClick()
      composeRule.runOnIdle { assertEquals(1, count.intValue) }
      composeRule.onNodeWithText("1").assertExists()
   }
}
