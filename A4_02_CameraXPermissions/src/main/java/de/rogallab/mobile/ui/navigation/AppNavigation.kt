package de.rogallab.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import de.rogallab.mobile.shared.ui.effects.SnackbarController
import de.rogallab.mobile.ui.camerax.comp.CameraAdapter
import de.rogallab.mobile.ui.videoviewer.comp.VideoViewerAdapter
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(
   snackbarController: SnackbarController
) {
   val backStack = rememberNavBackStack(CameraRoute)

   NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      // entryDecorators: take them over from your existing AppNavigation
      entryProvider = entryProvider {

         entry<CameraRoute> {
            CameraAdapter(
               onMessage = snackbarController::showMessage,
               onError = snackbarController::showError,
               onOpenViewer = { videoUri -> backStack.add(VideoViewerRoute(videoUri)) },
            )
         }

         entry<VideoViewerRoute> { route ->
            VideoViewerAdapter(
               viewModel = koinViewModel { parametersOf(route.videoUri) },
               onNavigateBack = { backStack.removeLastOrNull() },
            )
         }
      },
   )
}