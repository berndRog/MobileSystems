package de.rogallab.mobile.domain.utilities

// Normalizes an image reference stored by the app.
//
// Current images are private file paths or file/content URIs. Older
// android.resource references are ignored because they are not managed by the
// private image storage and can no longer be used as persistent user images.
fun String?.normalizedImagePath(): String? =
   this
      ?.trim()
      ?.takeIf { imagePath ->
         imagePath.isNotEmpty() &&
            !imagePath.startsWith(ANDROID_RESOURCE_PREFIX)
      }

// Removes empty, obsolete and duplicate image references while preserving
// the original order. The first remaining entry is therefore the preview image.
fun Iterable<String>.normalizedImagePaths(): List<String> =
   mapNotNull { imagePath -> imagePath.normalizedImagePath() }
      .distinct()

private const val ANDROID_RESOURCE_PREFIX = "android.resource://"
