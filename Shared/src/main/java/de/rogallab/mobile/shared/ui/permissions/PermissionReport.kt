package de.rogallab.mobile.shared.ui.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionEntry(
   val name: String,
   val type: String,          // normal | dangerous | signature/special
   val isGranted: Boolean,
)

fun Context.createPermissionsReport(): List<PermissionEntry> {
   val info = if (Build.VERSION.SDK_INT >= 33)
      packageManager.getPackageInfo(
         packageName,
         PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
   else
      @Suppress("DEPRECATION")
      packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)

   return info.requestedPermissions.orEmpty().map { name ->
      val type = try {
         when (packageManager.getPermissionInfo(name, 0).protection) {
            PermissionInfo.PROTECTION_NORMAL    -> "normal"
            PermissionInfo.PROTECTION_DANGEROUS -> "dangerous"
            else                                -> "signature/special"
         }
      } catch (e: PackageManager.NameNotFoundException) { "unknown" }
      PermissionEntry(
         name = name,
         type = type,
         isGranted = ContextCompat.checkSelfPermission(this, name) ==
            PackageManager.PERMISSION_GRANTED,
      )
   }
}