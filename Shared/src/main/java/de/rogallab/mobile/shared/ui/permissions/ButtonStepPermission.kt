package de.rogallab.mobile.shared.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun StepPermissionButton(
   onGranted: () -> Unit,
) {
   val context = LocalContext.current

   val permission = Manifest.permission.ACTIVITY_RECOGNITION

   val launcher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
   ) { granted ->
      if (granted) onGranted()
   }

   Button(onClick = {
      val granted = Build.VERSION.SDK_INT < 29 ||
         ContextCompat.checkSelfPermission(
            context, permission
         ) == PackageManager.PERMISSION_GRANTED

      if (granted) {
         onGranted()
      } else {
         launcher.launch(permission)
      }
   }) {
      Text("Schrittzähler aktivieren")
   }
}