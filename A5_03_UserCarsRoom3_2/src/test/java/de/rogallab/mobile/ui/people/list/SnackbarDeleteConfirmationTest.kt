package de.rogallab.mobile.ui.people.list

import androidx.compose.material3.SnackbarHostState
import de.rogallab.mobile.shared.ui.effects.SnackbarController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnackbarDeleteConfirmationTest {

   @Test
   fun dismissedConfirmation_doesNotInvokeDeleteAction() = runTest {
      val hostState = SnackbarHostState()
      val controller = SnackbarController(hostState, this)
      var deleteActions = 0

      controller.showAction("Delete Ada?", "Delete", onAction = { deleteActions++ })
      runCurrent()
      assertNotNull(hostState.currentSnackbarData)

      hostState.currentSnackbarData!!.dismiss()
      runCurrent()

      assertEquals(0, deleteActions)
   }

   @Test
   fun confirmedSnackbar_invokesDeleteActionExactlyOnce() = runTest {
      val hostState = SnackbarHostState()
      val controller = SnackbarController(hostState, this)
      var deleteActions = 0

      controller.showAction("Delete Ada?", "Delete", onAction = { deleteActions++ })
      runCurrent()
      assertNotNull(hostState.currentSnackbarData)

      hostState.currentSnackbarData!!.performAction()
      runCurrent()

      assertEquals(1, deleteActions)
   }
}
