package de.rogallab.mobile.ui.camerax

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Tests the composable's permission flow with controlled system results.
// No system dialog is opened and the device's real permissions are not changed.
@RunWith(AndroidJUnit4::class)
class CameraAndAudioPermissionsHandlerInstrumentedTest {
   @get:Rule val compose = createComposeRule()
   private val permissions = ControlledPermissions()

   @Test
   fun cameraAlreadyGranted_showsCameraWithoutAnotherRequest() {
      permissions.granted += Manifest.permission.CAMERA

      showHandler()

      compose.onNodeWithText("camera: audio=false").assertExists()
      assertEquals(0, permissions.requests.size)
   }

   @Test
   fun cameraAndAudioAlreadyGranted_enablesAudioWithoutAnotherRequest() {
      permissions.granted += Manifest.permission.CAMERA
      permissions.granted += Manifest.permission.RECORD_AUDIO

      showHandler()

      compose.onNodeWithText("camera: audio=true").assertExists()
      assertEquals(0, permissions.requests.size)
   }

   @Test
   fun grantingCameraButDenyingAudio_showsCameraWithoutSound() {
      showHandler()
      assertRequestedPermissions()
      compose.onNodeWithText("camera: audio=false").assertDoesNotExist()

      compose.runOnIdle {
         permissions.complete(cameraGranted = true, audioGranted = false)
      }

      compose.onNodeWithText("camera: audio=false").assertExists()
      assertEquals(1, permissions.requests.size)
   }

   @Test
   fun grantingBothPermissions_showsCameraWithSound() {
      showHandler()
      assertRequestedPermissions()

      compose.runOnIdle {
         permissions.complete(cameraGranted = true, audioGranted = true)
      }

      compose.onNodeWithText("camera: audio=true").assertExists()
      assertEquals(1, permissions.requests.size)
   }

   @Test
   fun denyingCamera_keepsCameraHiddenAndDoesNotRequestInALoop() {
      showHandler()
      assertRequestedPermissions()

      compose.runOnIdle {
         permissions.complete(cameraGranted = false, audioGranted = true)
      }

      val context = ApplicationProvider.getApplicationContext<Context>()
      compose.onNodeWithText(context.getString(R.string.permission_camera_rationale_preview))
         .assertExists()
      compose.onNodeWithText("camera: audio=true").assertDoesNotExist()
      compose.onNodeWithText("camera: audio=false").assertDoesNotExist()
      assertEquals(1, permissions.requests.size)
   }

   private fun showHandler() {
      compose.setContent {
         val context = LocalContext.current
         val permissionContext = object : ContextWrapper(context) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int =
               if (permission == Manifest.permission.CAMERA ||
                  permission == Manifest.permission.RECORD_AUDIO
               ) {
                  if (permission in permissions.granted) PackageManager.PERMISSION_GRANTED
                  else PackageManager.PERMISSION_DENIED
               } else super.checkPermission(permission, pid, uid)
         }
         CompositionLocalProvider(
            LocalContext provides permissionContext,
            LocalActivityResultRegistryOwner provides permissions,
         ) {
            CameraAndAudioPermissionsHandler { audioGranted ->
               Text("camera: audio=$audioGranted")
            }
         }
      }
      compose.waitForIdle()
   }

   private fun assertRequestedPermissions() {
      assertEquals(
         listOf(setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)),
         permissions.requests,
      )
   }

   private class ControlledPermissions : ActivityResultRegistryOwner {
      val granted = mutableSetOf<String>()
      val requests = mutableListOf<Set<String>>()
      private var requestCode: Int? = null

      override val activityResultRegistry = object : ActivityResultRegistry() {
         override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
         ) {
            this@ControlledPermissions.requestCode = requestCode
            @Suppress("UNCHECKED_CAST")
            requests += (input as Array<String>).toSet()
         }
      }

      fun complete(cameraGranted: Boolean, audioGranted: Boolean) {
         val result = mapOf(
            Manifest.permission.CAMERA to cameraGranted,
            Manifest.permission.RECORD_AUDIO to audioGranted,
         )
         result.forEach { (permission, allowed) ->
            if (allowed) granted += permission else granted -= permission
         }
         activityResultRegistry.dispatchResult(requireNotNull(requestCode), result)
         requestCode = null
      }
   }
}
