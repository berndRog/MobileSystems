package de.rogallab.mobile.shared.ui.images

import android.net.Uri
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.IImageEditDelegate
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.ImageChange
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.resultOf
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class ImageEditDelegate(
   private val session: IImageEdit,
   private val storage: IImageFileStorage,
   private val mode: ImageSelectionMode,
   private val maxImages: Int,
) : IImageEditDelegate {

   init { require(maxImages > 0) { "maxImages must be positive" } }

   // Ownership history, not a second copy of the current selection.
   private val knownPaths = mutableSetOf<String>()

   override fun start(originalPaths: List<String>) {
      knownPaths.addAll(originalPaths)
      session.start(originalPaths)
   }

   override suspend fun importGallery(uris: List<Uri>): Result<ImageChange> = resultOf(block = {
      val pending = mutableListOf<String>()
      var failed = 0
      var firstFailure: Throwable? = null
      try {
         uris.forEach { uri ->
            currentCoroutineContext().ensureActive()
            storage.copyImageToAppStorage(uri)
               .onSuccess { path ->
                  if (knownPaths.add(path)) pending += path
               }
               .onFailure { error ->
                  if (error is CancellationException) throw error
                  failed++
                  if (firstFailure == null) firstFailure = error
               }
            currentCoroutineContext().ensureActive()
         }
         if (pending.isEmpty()) {
            if (failed > 0) throw IllegalStateException("No images could be imported", firstFailure)
            ImageChange()
         } else {
            val incoming = pending.toList()
            // ImageEdit owns these files from entry into applyImages, including overflow cleanup.
            pending.clear()
            ImageChange(paths = applyImages(incoming), failedImports = failed)
         }
      } finally {
         cleanupUnadopted(pending)
      }
   })

   override suspend fun acceptCamera(path: String): Result<ImageChange> = resultOf(block = {
      if (!claimCamera(path)) return@resultOf ImageChange()
      var handedOver = false
      try {
         currentCoroutineContext().ensureActive()
         handedOver = true
         ImageChange(paths = applyImages(listOf(path)))
      } finally {
         if (!handedOver) cleanupUnadopted(listOf(path))
      }
   })

   override suspend fun rejectCamera(path: String): Result<Unit> {
      // Claim before suspending: duplicate results must not adopt a file being deleted.
      if (!claimCamera(path)) return Result.success(Unit)
      return deleteUnadopted(path).onFailure { error ->
         Alog.e("<-ImageEditDelegate", "Rejected camera cleanup failed", error)
      }
   }

   override suspend fun remove(path: String): Result<ImageChange> =
      resultOf(block = { ImageChange(paths = session.remove(path)) })

   override suspend fun discard(): Result<Unit> =
      resultOf(block = { session.discard() })

   private fun claimCamera(path: String): Boolean =
      path.isNotBlank() && knownPaths.add(path)

   private suspend fun applyImages(paths: List<String>): List<String> {
      val selection = when (mode) {
         ImageSelectionMode.APPEND -> session.add(paths)
         ImageSelectionMode.REPLACE -> session.replace(paths)
      }
      return if (selection.size > maxImages) {
         session.replace(selection.take(maxImages))
      } else selection
   }

   private suspend fun deleteUnadopted(path: String): Result<Unit> = resultOf(block = {
      // Only known, unadopted files are cleaned after caller cancellation.
      withContext(NonCancellable) {
         storage.deleteImageFromAppStorage(path).getOrThrow()
      }
   })

   private suspend fun cleanupUnadopted(paths: List<String>) {
      if (paths.isEmpty()) return
      withContext(NonCancellable) {
         paths.forEach { path ->
            resultOf(block = { storage.deleteImageFromAppStorage(path).getOrThrow() })
               .onFailure { error ->
                  Alog.e("<-ImageEditDelegate", "Temporary image cleanup failed", error)
               }
         }
      }
   }
}
