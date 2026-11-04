package de.rogallab.mobile.ui.layout

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ExampleColumnTest {
   @get:Rule val composeRule = createComposeRule()

   @Test fun columnDisplaysHeadingAndStacksThreeBoxesInOrder() {
      composeRule.setContent { ExampleColumn() }

      composeRule.onNodeWithText("Column with three boxes").assertExists()
      val red = composeRule.onNodeWithTag("redBox").getUnclippedBoundsInRoot()
      val green = composeRule.onNodeWithTag("greenBox").getUnclippedBoundsInRoot()
      val blue = composeRule.onNodeWithTag("blueBox").getUnclippedBoundsInRoot()

      org.junit.Assert.assertTrue(red.bottom <= green.top)
      org.junit.Assert.assertTrue(green.bottom <= blue.top)
   }
}
