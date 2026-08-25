package de.rogallab.mobile.shared.ui.common

import android.net.Uri
import java.io.File

// Creates a Coil model for private paths and URI-based image references.
fun String.toImageModel(): Any =
   when {
      startsWith("content://") ||
         startsWith("file://") -> Uri.parse(this)

      else -> File(this)
   }
