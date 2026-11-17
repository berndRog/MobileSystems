package de.rogallab.mobile.ui.videoviewer

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class VideoViewerViewModelTest {
   @Test
   fun withoutInitialVideo_startsInSelectionState() {
      val viewModel = VideoViewerViewModel(initialVideoUri = null)

      assertNull(viewModel.stateFlow.value.videoUri)
   }

   @Test
   fun initialVideo_isAvailableToPlayer() {
      val uri = "content://test/videos/last-recording"
      val viewModel = VideoViewerViewModel(initialVideoUri = uri)

      assertEquals(Uri.parse(uri), viewModel.stateFlow.value.videoUri)
   }

   @Test
   fun selectingAnotherVideo_replacesCurrentVideo() {
      val viewModel = VideoViewerViewModel("content://test/videos/first")
      val selected = Uri.parse("content://test/videos/second")

      viewModel.onIntent(VideoViewerIntent.VideoSelected(selected))

      assertEquals(selected, viewModel.stateFlow.value.videoUri)
   }

   @Test
   fun clearingVideo_returnsToSelectionAndAllowsSelectingAgain() {
      val viewModel = VideoViewerViewModel("content://test/videos/first")

      viewModel.onIntent(VideoViewerIntent.ClearVideo)

      assertNull(viewModel.stateFlow.value.videoUri)

      val selected = Uri.parse("content://test/videos/second")
      viewModel.onIntent(VideoViewerIntent.VideoSelected(selected))

      assertEquals(selected, viewModel.stateFlow.value.videoUri)
   }
}
