package de.rogallab.mobile.shared.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.rogallab.mobile.shared.R

@Composable
fun CameraPermissionHandler(
   onGranted: () -> Unit,
   content: @Composable (requestCamera: () -> Unit) -> Unit,
) {
   val context = LocalContext.current
   val currentOnGranted by rememberUpdatedState(onGranted)
   var showRationale by rememberSaveable { mutableStateOf(false) }
   var showSettingsHint by rememberSaveable { mutableStateOf(false) }

   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
   ) { granted ->
      if (granted) {
         currentOnGranted()
      } else {
         // After a denial, "false" means the system will not ask again.
         val canAskAgain = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
               it, Manifest.permission.CAMERA)
         } ?: false
         if (canAskAgain) showRationale = true else showSettingsHint = true
      }
   }

   val request = {
      val granted = ContextCompat.checkSelfPermission(
         context, Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
      if (granted) currentOnGranted() else launcher.launch(Manifest.permission.CAMERA)
   }

   if (showRationale) {
      AlertDialog(
         onDismissRequest = { showRationale = false },
         text = { Text(stringResource(R.string.permission_camera_rationale)) },
         confirmButton = {
            TextButton(onClick = {
               showRationale = false
               launcher.launch(Manifest.permission.CAMERA)
            }) { Text(stringResource(R.string.action_ok)) }
         },
         dismissButton = {
            TextButton(onClick = { showRationale = false }) {
               Text(stringResource(R.string.action_cancel))
            }
         },
      )
   }
   if (showSettingsHint) {
      AlertDialog(
         onDismissRequest = { showSettingsHint = false },
         text = { Text(stringResource(R.string.permission_camera_settings)) },
         confirmButton = {
            TextButton(onClick = {
               showSettingsHint = false
               context.startActivity(
                  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                     Uri.fromParts("package", context.packageName, null)))
            }) { Text(stringResource(R.string.action_open_settings)) }
         },
         dismissButton = {
            TextButton(onClick = { showRationale = false }) {
               Text(stringResource(R.string.action_cancel))
            }
         },
      )
   }
   content(request)
}

// Helper: a Compose context is often wrapped.
fun Context.findActivity(): Activity? = when (this) {
   is Activity -> this
   is ContextWrapper -> baseContext.findActivity()
   else -> null
}
