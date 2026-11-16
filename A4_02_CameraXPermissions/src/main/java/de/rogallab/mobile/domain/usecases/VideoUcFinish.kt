package de.rogallab.mobile.domain.usecases

import android.net.Uri
import de.rogallab.mobile.shared.domain.images.IVideoMediaStore
import de.rogallab.mobile.shared.domain.images.PendingVideo
import de.rogallab.mobile.shared.domain.utilities.Alog

/**
 * Completes a video recording.
 *
 * A successful recording is published, a failed one is discarded, so that no
 * hidden or incomplete entries remain in the MediaStore.
 */
class VideoUcFinish(
   private val _videoMediaStore: IVideoMediaStore,
) {
   suspend operator fun invoke(
      pendingVideo: PendingVideo,
      recordedSuccessfully: Boolean,
   ): Result<Uri> {

      // Writing has finished, the descriptor is no longer needed.
      pendingVideo.close()

      if (!recordedSuccessfully) {
         _videoMediaStore.deleteVideoFromMediaStore(pendingVideo.uri)
            .onFailure {
               Alog.e(TAG, "discard video failed: ${it.message}")
            }
         return Result.failure(IllegalStateException("The recording failed."))
      }

      return _videoMediaStore.publishVideo(pendingVideo.uri)
         .onFailure {
            // A video that cannot be published must not stay hidden in the MediaStore.
            Alog.e(TAG, "publishVideo failed: ${it.message}")
            _videoMediaStore.deleteVideoFromMediaStore(pendingVideo.uri)
         }
   }

   companion object {
      private const val TAG = "<-VideoUcFinish"
   }
}