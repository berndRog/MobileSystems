package de.rogallab.mobile.shared.domain.images

import android.net.Uri

// One delegate per edit session. The caller serializes selection changes.
interface IImageEditDelegate {
   fun start(originalPaths: List<String>)
   suspend fun importGallery(uris: List<Uri>): Result<ImageChange>
   suspend fun acceptCamera(path: String): Result<ImageChange>
   suspend fun rejectCamera(path: String): Result<Unit>
   suspend fun remove(path: String): Result<ImageChange>
   suspend fun discard(): Result<Unit>
}
