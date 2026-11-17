package de.rogallab.mobile.ui.camerax

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.R
import de.rogallab.mobile.ui.camerax.comp.CameraScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenInstrumentedTest {
   @get:Rule val compose = createComposeRule()
   private val context = ApplicationProvider.getApplicationContext<Context>()
   private val events = mutableListOf<String>()

   @Test
   fun idleCamera_buttonsForwardPhotoRecordingAndViewerEvents() {
      showScreen(isRecording = false, audioGranted = true)

      compose.onNodeWithContentDescription(text(R.string.action_take_photo)).performClick()
      compose.onNodeWithContentDescription(text(R.string.action_start_video)).performClick()
      compose.onNodeWithContentDescription(text(R.string.action_open_videos)).performClick()

      compose.runOnIdle {
         assertEquals(listOf("photo", "recording", "viewer"), events)
      }
   }

   @Test
   fun recording_disablesPhotoAndViewerButAllowsStopping() {
      showScreen(isRecording = true, audioGranted = true)

      compose.onNodeWithContentDescription(text(R.string.action_take_photo)).assertIsNotEnabled()
      compose.onNodeWithContentDescription(text(R.string.action_open_videos)).assertIsNotEnabled()
      compose.onNodeWithText(text(R.string.camera_recording)).assertExists()
      compose.onNodeWithContentDescription(text(R.string.action_stop_video))
         .assertIsEnabled()
         .performClick()

      compose.runOnIdle { assertEquals(listOf("recording"), events) }
   }

   @Test
   fun deniedAudio_showsHintAndStillAllowsRecording() {
      showScreen(isRecording = false, audioGranted = false)

      compose.onNodeWithText(text(R.string.camera_no_audio_hint)).assertExists()
      compose.onNodeWithContentDescription(text(R.string.action_start_video))
         .assertIsEnabled()
         .performClick()

      compose.runOnIdle { assertEquals(listOf("recording"), events) }
   }

   private fun showScreen(isRecording: Boolean, audioGranted: Boolean) {
      compose.setContent {
         CameraScreen(
            surfaceRequest = null, // No real camera needed for control behaviour.
            isRecording = isRecording,
            audioGranted = audioGranted,
            onTakePhoto = { events += "photo" },
            onToggleRecording = { events += "recording" },
            onOpenViewer = { events += "viewer" },
         )
      }
   }

   private fun text(resId: Int): String = context.getString(resId)
}
