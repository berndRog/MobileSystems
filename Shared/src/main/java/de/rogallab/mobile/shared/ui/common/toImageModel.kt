package de.rogallab.mobile.shared.ui.common

import android.net.Uri
import java.io.File

// Creates a Coil model for private paths, URI-based and web image references.
fun String.toImageModel(): Any =
   when {
      startsWith("content://") ||
         startsWith("file://") -> Uri.parse(this)

      startsWith("http://") ||
         startsWith("https://") -> this

      else -> File(this)
   }
