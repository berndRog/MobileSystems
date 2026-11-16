package de.rogallab.mobile.shared.domain.images

import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.Closeable

/**
 * A video entry that already exists in the MediaStore but is not yet visible
 * to other apps.
 *
 * @property uri Content Uri of the hidden MediaStore entry.
 * @property fileDescriptor Writable and seekable descriptor of the entry.
 *
 * The descriptor must be closed when writing has finished. Closing the
 * PendingVideo does exactly that.
 */
class PendingVideo(
   val uri: Uri,
   val fileDescriptor: ParcelFileDescriptor,
) : Closeable {

   override fun close() {
      // Closing twice or after a failure must never crash the caller.
      runCatching { fileDescriptor.close() }
   }
}

/**
 * Provides video operations for Android's shared MediaStore.
 *
 * Videos are written in two steps: first a hidden entry is created and filled,
 * then it is published. Only published videos are visible to gallery apps.
 */
interface IVideoMediaStore {

   // Creates a hidden MediaStore entry below the public Movies collection and
   // opens it for writing.
   //
   // groupName: Subdirectory below Movies, for example "A4_02_CameraX".
   // fileName: Optional public name. The extension is always ".mp4".
   // result<PendingVideo> with the content Uri and a writable descriptor.
   suspend fun createPendingVideo(
      groupName: String,
      fileName: String? = null,
   ): Result<PendingVideo>

   // Makes a completely written video visible to other apps.
   suspend fun publishVideo(videoUri: Uri): Result<Uri>

   // Deletes one concrete MediaStore video Uri owned by the app.
   // A missing item is treated as already deleted.
   suspend fun deleteVideoFromMediaStore(videoUri: Uri): Result<Unit>
}