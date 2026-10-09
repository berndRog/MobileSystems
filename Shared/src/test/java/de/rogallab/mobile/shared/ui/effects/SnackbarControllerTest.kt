package de.rogallab.mobile.shared.ui.effects

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import de.rogallab.mobile.shared.domain.utilities.Alog
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarControllerTest {

   @Test
   fun showMessage_showsResolvedString() = runTest {
      val hostState = SnackbarHostState()
      val controller = SnackbarController(hostState, this)

      controller.showMessage("saved")
      runCurrent()

      assertEquals("saved", hostState.currentSnackbarData?.visuals?.message)

      hostState.currentSnackbarData?.dismiss()
      runCurrent()
   }

   @Test
   fun multipleMessages_areSerializedBySnackbarHostState() = runTest {
      val hostState = SnackbarHostState()
      val controller = SnackbarController(hostState, this)

      controller.showMessage("first")
      controller.showMessage("second")
      runCurrent()

      assertEquals("first", hostState.currentSnackbarData?.visuals?.message)

      hostState.currentSnackbarData?.dismiss()
      runCurrent()

      assertEquals("second", hostState.currentSnackbarData?.visuals?.message)

      hostState.currentSnackbarData?.dismiss()
      runCurrent()
   }

   @Test
   fun showError_usesLongDurationAndDismissAction() = runTest {
      val hostState = SnackbarHostState()
      val controller = SnackbarController(hostState, this)

      Alog.set(useAndroidLog = false)
      try {
         val output = ByteArrayOutputStream()
         val originalOut = System.out
         try {
            System.setOut(PrintStream(output))
            controller.showError("load failed")
         } finally {
            System.setOut(originalOut)
         }
         runCurrent()

         assertTrue(output.toString().contains("E/<-SnackbarController: load failed"))
         assertEquals("load failed", hostState.currentSnackbarData?.visuals?.message)
         assertEquals(SnackbarDuration.Long, hostState.currentSnackbarData?.visuals?.duration)
         assertTrue(hostState.currentSnackbarData?.visuals?.withDismissAction == true)

         hostState.currentSnackbarData?.dismiss()
         runCurrent()
      } finally {
         Alog.reset()
      }
   }

   @Test
   fun showAction_actionPerformedCallsOnActionOnly() = runTest {
      val hostState = SnackbarHostState()
      var actionCalled = false
      var dismissCalled = false
      val controller = SnackbarController(hostState, this)

      controller.showAction(
         message = "removed",
         actionLabel = "undo",
         onAction = { actionCalled = true },
         onDismiss = { dismissCalled = true },
      )
      runCurrent()

      assertEquals("undo", hostState.currentSnackbarData?.visuals?.actionLabel)
      hostState.currentSnackbarData?.performAction()
      runCurrent()

      assertTrue(actionCalled)
      assertFalse(dismissCalled)
   }

   @Test
   fun showAction_dismissedCallsOnDismissOnly() = runTest {
      val hostState = SnackbarHostState()
      var actionCalled = false
      var dismissCalled = false
      val controller = SnackbarController(hostState, this)

      controller.showAction(
         message = "removed",
         actionLabel = "undo",
         onAction = { actionCalled = true },
         onDismiss = { dismissCalled = true },
      )
      runCurrent()

      hostState.currentSnackbarData?.dismiss()
      runCurrent()

      assertFalse(actionCalled)
      assertTrue(dismissCalled)
   }
}
