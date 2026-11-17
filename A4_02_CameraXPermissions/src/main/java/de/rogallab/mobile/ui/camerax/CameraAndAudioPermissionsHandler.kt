package de.rogallab.mobile.ui.camerax

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.rogallab.mobile.ui.findActivity
import de.rogallab.mobile.R

@Composable
fun CameraAndAudioPermissionsHandler(
   modifier: Modifier = Modifier,
   content: @Composable (audioGranted: Boolean) -> Unit,
) {
   val context = LocalContext.current

   fun isGranted(permission: String) =
      ContextCompat.checkSelfPermission(context, permission) ==
         PackageManager.PERMISSION_GRANTED

   var cameraGranted by remember { mutableStateOf(isGranted(Manifest.permission.CAMERA)) }
   var audioGranted by remember { mutableStateOf(isGranted(Manifest.permission.RECORD_AUDIO)) }

   // false after a permanent denial: the system shows no dialog anymore
   var canAskAgain by rememberSaveable { mutableStateOf(true) }

   fun refresh() {
      cameraGranted = isGranted(Manifest.permission.CAMERA)
      audioGranted = isGranted(Manifest.permission.RECORD_AUDIO)
   }

   val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
   ) {
      refresh()
      if (!cameraGranted) {
         canAskAgain = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
               it, Manifest.permission.CAMERA)
         } ?: false
      }
   }

   // The user may come back from the system settings.
   LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
      refresh()
   }

   // Ask once when the screen is shown.
   LaunchedEffect(Unit) {
      if (!cameraGranted) launcher.launch(permissions)
   }

   // The modifier is applied exactly once, to a container around both branches.
   Box(
      modifier = modifier.fillMaxSize()
   ) {
      // Permissions are already granted
      if (cameraGranted) {
         content(audioGranted)
      }
      // Request permissions
      else {
         Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Text(
               text = stringResource(R.string.permission_camera_rationale_preview),
               textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (canAskAgain) {
               Button(
                  onClick = {
                     launcher.launch(permissions)
                  }
               ) {
                  Text(stringResource(R.string.action_grant_permission))
               }
            } else {
               Button(onClick = {
                  context.startActivity(
                     Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                     )
                  )
               }) { Text(stringResource(R.string.action_open_settings)) }
            }
         }
      }
   }
}