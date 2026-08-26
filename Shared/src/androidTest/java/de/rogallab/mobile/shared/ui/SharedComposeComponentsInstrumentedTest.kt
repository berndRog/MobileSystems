package de.rogallab.mobile.shared.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.rogallab.mobile.shared.R
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.shared.ui.images.ImageRenderer
import de.rogallab.mobile.shared.ui.images.ImageSelection
import de.rogallab.mobile.shared.ui.images.ImageSelectionButtons
import java.io.File
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedComposeComponentsInstrumentedTest {

   @get:Rule
   val composeRule = createComposeRule()

   private val context
      get() = InstrumentationRegistry
         .getInstrumentation()
         .targetContext

   @Test
   fun imageSelectionButtons_withoutImage_hidesRemoveButton() {
      val removeText = context.getString(R.string.action_remove_photo)

      composeRule.setContent {
         MaterialTheme {
            ImageSelectionButtons(
               imagePath = null,
               onSelectPhoto = {},
               onTakePhoto = {},
               onRemovePhoto = {},
            )
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNodeWithText(removeText)
         .assertDoesNotExist()
   }

   @Test
   fun imageSelectionButtons_withImage_showsAllActionsAndDelegatesClicks() {
      val selectText = context.getString(R.string.action_select_photo)
      val cameraText = context.getString(R.string.action_take_photo)
      val removeText = context.getString(R.string.action_remove_photo)
      var selectCalls = 0
      var cameraCalls = 0
      var removeCalls = 0

      composeRule.setContent {
         MaterialTheme {
            Column {
               ImageSelectionButtons(
                  imagePath = "/images/person.jpg",
                  onSelectPhoto = { selectCalls++ },
                  onTakePhoto = { cameraCalls++ },
                  onRemovePhoto = { removeCalls++ },
               )
            }
         }
      }
      composeRule.waitForIdle()

      composeRule.onNodeWithText(selectText).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithText(cameraText).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithText(removeText).performClick()
      composeRule.waitForIdle()

      composeRule.runOnIdle {
         assertEquals(1, selectCalls)
         assertEquals(1, cameraCalls)
         assertEquals(1, removeCalls)
      }
   }

   @Test
   fun imageSelectionButtons_disabled_disablesAllVisibleActions() {
      val selectText = context.getString(R.string.action_select_photo)
      val cameraText = context.getString(R.string.action_take_photo)
      val removeText = context.getString(R.string.action_remove_photo)

      composeRule.setContent {
         MaterialTheme {
            ImageSelectionButtons(
               imagePath = "/images/person.jpg",
               enabled = false,
               onSelectPhoto = {},
               onTakePhoto = {},
               onRemovePhoto = {},
            )
         }
      }
      composeRule.waitForIdle()

      composeRule.onNodeWithText(selectText).assertIsNotEnabled()
      composeRule.onNodeWithText(cameraText).assertIsNotEnabled()
      composeRule.onNodeWithText(removeText).assertIsNotEnabled()
   }

   @Test
   fun imageSelectionButtons_enabled_enablesAllVisibleActions() {
      val selectText = context.getString(R.string.action_select_photo)
      val cameraText = context.getString(R.string.action_take_photo)
      val removeText = context.getString(R.string.action_remove_photo)

      composeRule.setContent {
         MaterialTheme {
            ImageSelectionButtons(
               imagePath = "/images/person.jpg",
               enabled = true,
               onSelectPhoto = {},
               onTakePhoto = {},
               onRemovePhoto = {},
            )
         }
      }
      composeRule.waitForIdle()

      composeRule.onNodeWithText(selectText).assertIsEnabled()
      composeRule.onNodeWithText(cameraText).assertIsEnabled()
      composeRule.onNodeWithText(removeText).assertIsEnabled()
   }

   @Test
   fun imageRenderer_withoutPath_showsPlaceholderWithContentDescription() {
      composeRule.setContent {
         MaterialTheme {
            ImageRenderer(
               imageVector = Icons.Default.AccountCircle,
               imagePath = null,
               contentDescription = "Ada Lovelace",
            )
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNodeWithContentDescription("Ada Lovelace")
         .assertExists()
   }

   @Test
   fun imageRenderer_withPath_exposesImageContentDescription() {
      val imageFile = File(context.cacheDir, "compose-image.png")
      val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
      imageFile.outputStream().use { output ->
         bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
      }
      bitmap.recycle()

      try {
         composeRule.setContent {
            MaterialTheme {
               ImageRenderer(
                  modifier = Modifier.size(120.dp),
                  imageVector = Icons.Default.AccountCircle,
                  imagePath = imageFile.absolutePath,
                  contentDescription = "Ada Lovelace",
               )
            }
         }
         composeRule.waitForIdle()

         composeRule
            .onNodeWithContentDescription("Ada Lovelace")
            .assertExists()
      }
      finally {
         imageFile.delete()
      }
   }

   @Test
   fun imageSelection_delegatesImageActions() {
      val selectText = context.getString(R.string.action_select_photo)
      val cameraText = context.getString(R.string.action_take_photo)
      val removeText = context.getString(R.string.action_remove_photo)
      var selectCalls = 0
      var cameraCalls = 0
      var removeCalls = 0

      composeRule.setContent {
         MaterialTheme {
            ImageSelection(
               fullName = "Ada Lovelace",
               imagePath = "/images/person.jpg",
               onSelectPhoto = { selectCalls++ },
               onTakePhoto = { cameraCalls++ },
               onRemovePhoto = { removeCalls++ },
            )
         }
      }
      composeRule.waitForIdle()

      composeRule.onNodeWithText(selectText).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithText(cameraText).performClick()
      composeRule.waitForIdle()
      composeRule.onNodeWithText(removeText).performClick()
      composeRule.waitForIdle()

      composeRule.runOnIdle {
         assertEquals(1, selectCalls)
         assertEquals(1, cameraCalls)
         assertEquals(1, removeCalls)
      }
   }

   @Test
   fun inputValueString_textInputIsHoistedToCaller() {
      var value by mutableStateOf("")

      composeRule.setContent {
         MaterialTheme {
            InputValueString(
               value = value,
               onValueChange = { value = it },
               label = "First name",
            )
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNode(hasSetTextAction())
         .performTextInput("Ada")
      composeRule.waitForIdle()

      composeRule.runOnIdle {
         assertEquals("Ada", value)
      }
   }

   @Test
   fun inputValueString_doneShowsValidationError() {
      var value by mutableStateOf("A")

      composeRule.setContent {
         MaterialTheme {
            InputValueString(
               value = value,
               onValueChange = { value = it },
               label = "First name",
               validate = { input ->
                  if (input.length < 2) "Too short" else null
               },
            )
         }
      }
      composeRule.waitForIdle()

      composeRule
         .onNode(hasSetTextAction())
         .performClick()
         .performImeAction()
      composeRule.waitForIdle()

      composeRule
         .onNodeWithText("Too short")
         .assertExists()
   }

   @Test
   fun inputValueString_newInputHidesPreviousValidationError() {
      var value by mutableStateOf("A")

      composeRule.setContent {
         MaterialTheme {
            InputValueString(
               value = value,
               onValueChange = { value = it },
               label = "First name",
               validate = { input ->
                  if (input.length < 2) "Too short" else null
               },
            )
         }
      }
      composeRule.waitForIdle()

      val textField = composeRule.onNode(hasSetTextAction())
      textField.performClick()
      textField.performImeAction()
      composeRule.waitForIdle()
      composeRule.onNodeWithText("Too short").assertExists()

      textField.performTextReplacement("Ada")
      composeRule.waitForIdle()

      composeRule.onNodeWithText("Too short").assertDoesNotExist()
   }

   @Test
   fun effectHandler_collectsEffectsInOrder() {
      val received = mutableListOf<String>()

      composeRule.setContent {
         EffectHandler(
            effects = flowOf("first", "second"),
            onEffect = { effect -> received += effect },
         )
      }
      composeRule.waitForIdle()

      composeRule.waitUntil(timeoutMillis = 2_000) {
         received.size == 2
      }

      composeRule.runOnIdle {
         assertEquals(listOf("first", "second"), received)
      }
   }
}
