package de.rogallab.mobile.ui.navigation.composables

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil.ImageLoader
import de.rogallab.mobile.Globals
import de.rogallab.mobile.domain.utilities.logComp
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logVerbose
import de.rogallab.mobile.ui.features.article.ArticlesViewModel
import de.rogallab.mobile.ui.features.article.composables.ArticleWebScreen
import de.rogallab.mobile.ui.features.article.composables.ArticlesListScreen
import de.rogallab.mobile.ui.features.news.NewsViewModel
import de.rogallab.mobile.ui.features.news.composables.NewsListScreen
import de.rogallab.mobile.ui.navigation.ArticleWeb
import de.rogallab.mobile.ui.navigation.ArticlesList
import de.rogallab.mobile.ui.navigation.Nav3ViewModelTopLevel
import de.rogallab.mobile.ui.navigation.NewsList
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.core.parameter.parametersOf
import kotlin.collections.listOf

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppNavigation(
   // navViewModel is Activity-scoped
   navViewModel: Nav3ViewModelTopLevel = koinActivityViewModel { parametersOf(NewsList) },
   newsViewModel: NewsViewModel = koinActivityViewModel<NewsViewModel>{ parametersOf(navViewModel) },
   articlesViewModel: ArticlesViewModel = koinActivityViewModel<ArticlesViewModel>{ parametersOf(navViewModel) },
   imageLoader: ImageLoader = koinInject<ImageLoader>(),
   animationDuration: Int = Globals.animationDuration
) {
   val tag = "<-AppNavigation"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${nComp.value++}") }

   // Get the LifecycleOwner and Lifecycle
   val lifecycleOwner = (LocalActivity.current as? ComponentActivity)
      ?: LocalLifecycleOwner.current
   val lifecycle = lifecycleOwner.lifecycle
   SideEffect {
      logVerbose(tag, "lifecycleOwner:${lifecycleOwner.toString()} lifecycle.State:${lifecycle.currentState.toString()}")
   }

   // Use the navViewModel's backStack to manage navigation state
   val backStack = navViewModel.currentBackStack

   NavDisplay(
      backStack = backStack,
      onBack = {
         logDebug(tag, "onBack() - Backstack size: ${backStack.size}")
         navViewModel.pop()
      },
      entryDecorators = listOf(
         // Save/restore per-entry UI state
         rememberSaveableStateHolderNavEntryDecorator(
            rememberSaveableStateHolder()
         ),
         // Enables entry-scoped ViewModels if you also want them somewhere else
         rememberViewModelStoreNavEntryDecorator()
      ),
      // Standard Android navigation animations:
      // transitionSpec:    New screen slides in from the right ({ it }),
      //                    old slides out to the left ({ -it }).
      // popTransitionSpec: New screen slides in from the left ({ -it }),
      //                    old slides out to the right ({ it }).
      transitionSpec = {
         slideInHorizontally(
            animationSpec = tween(animationDuration)
         ){ it } togetherWith
         slideOutHorizontally(
            animationSpec = tween(animationDuration)
         ){ -it }
      },
      popTransitionSpec = {
         slideInHorizontally(
            animationSpec = tween(animationDuration)
         ){ -it } togetherWith
            slideOutHorizontally(
               animationSpec = tween(animationDuration)
            ){ it }
      },
      //
      predictivePopTransitionSpec = {
         slideIntoContainer(
            AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(animationDuration)
         ) togetherWith
            fadeOut(animationSpec = tween(animationDuration*3/2 ))
      },

      entryProvider = entryProvider {

         entry<NewsList> { _ ->
            NewsListScreen(
               newsViewModel = newsViewModel,
               articlesViewModel = articlesViewModel,
               imageLoader = imageLoader,
//               onNavigatePersonDetail = { personId ->
//                  navViewModel.push(PersonDetail(personId))
//               }
            )
         }
         entry<ArticlesList> { _ ->
            ArticlesListScreen(
               viewModel = articlesViewModel,
               imageLoader = imageLoader,
               onNavigateTopLevel = { navKey -> navViewModel.switchTopLevel(navKey) }
            )
         }
         entry<ArticleWeb> { key ->
            ArticleWebScreen(
               viewModel = articlesViewModel,
               onNavigateReverse = navViewModel::pop
            )
         }
      },
   )
}