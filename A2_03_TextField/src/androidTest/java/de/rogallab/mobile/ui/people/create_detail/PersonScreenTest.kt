package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PersonScreenTest {
   @get:Rule val composeRule = createComposeRule()

   @Test fun saveIsEnabledOnlyWhenBothNamesAreEntered() {
      val firstName = mutableStateOf("")
      val lastName = mutableStateOf("")
      var saves = 0
      composeRule.setContent {
         PersonScreen(
            firstName = firstName.value,
            onFirstNameChange = { firstName.value = it },
            lastName = lastName.value,
            onLastNameChange = { lastName.value = it },
            onSave = { saves++ },
            onCancel = {},
            modifier = Modifier,
         )
      }

      composeRule.onNodeWithText("Speichern").assertIsNotEnabled()
      composeRule.onNodeWithText("Vorname").performTextInput("Ada")
      composeRule.onNodeWithText("Speichern").assertIsNotEnabled()
      composeRule.onNodeWithText("Nachname").performTextInput("Lovelace")
      composeRule.onNodeWithText("Speichern").assertIsEnabled().performClick()
      composeRule.runOnIdle { assertEquals(1, saves) }
   }
}
