package de.rogallab.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.SnackbarController
import de.rogallab.mobile.ui.camerax.comp.CameraAdapter
import de.rogallab.mobile.ui.videoviewer.comp.VideoViewerAdapter
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
   snackbarController: SnackbarController
) {
   val backStack = rememberNavBackStack(CameraKey)

   NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      // entryDecorators: take them over from your existing AppNavigation
      entryProvider = entryProvider {

         entry<CameraKey> {
            CameraAdapter(
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onOpenViewer = { videoUri ->
                  Alog.v("<-AppNavigation", "open viewer, videoUri = $videoUri")
                  backStack.add(VideoViewerKey(videoUri))
               },
            )
         }

         entry<VideoViewerKey> { route ->
            VideoViewerAdapter(
               viewModel = koinViewModel { parametersOf(route.videoUri) },
               onNavigateBack = { backStack.removeLastOrNull() },
            )
         }
      },
   )
}