package de.rogallab.mobile.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

import de.rogallab.mobile.Globals.animationDuration

object NavigationAnimations {

   // Standard Android navigation animations
   // transitionSpec:    New screen slides in from the right ({ it }),
   //                    old slides out to the left ({ -it }).
   // popTransitionSpec: New screen slides in from the left ({ -it }),
   //                    old slides out to the right ({ it }).
   //
   // Forward navigation:
   // - The new screen enters from the right.
   // - The current screen exits to the left.
   //
   // Back navigation:
   // - The previous screen enters from the left.
   // - The current screen exits to the right.

   val enterTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      slideInHorizontally(
         initialOffsetX = { fullWidth -> fullWidth },
         animationSpec = tween(animationDuration),
      ) togetherWith slideOutHorizontally(
         targetOffsetX = { fullWidth -> -fullWidth },
         animationSpec = tween(animationDuration),
      )
   }

   fun popTransitionSpec(
      popReason: PopReason
   ): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      when (popReason) {
         PopReason.SAVE -> {
            slideInHorizontally(
               initialOffsetX = { fullWidth -> -fullWidth },
               animationSpec = tween(animationDuration),
            ) togetherWith slideOutHorizontally(
               targetOffsetX = { fullWidth -> fullWidth },
               animationSpec = tween(animationDuration),
            )
         }
         PopReason.CANCEL -> {
            fadeIn(
               animationSpec = tween(animationDuration)
            ) togetherWith (
               slideOutVertically(
                  targetOffsetY = { fullHeight -> fullHeight },
                  animationSpec = tween(animationDuration)
               ) + fadeOut(animationSpec = tween(animationDuration))
               )
         }
      }
   }

   val predictivePopTransitionSpec: AnimatedContentTransitionScope<*>.(Int) -> ContentTransform = { _ ->
      (scaleIn(
         initialScale = 0.9f,
         animationSpec = tween(animationDuration)
      ) + fadeIn(
         animationSpec = tween(animationDuration)
      )) togetherWith (
         scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(animationDuration)
         ) + fadeOut(
            animationSpec = tween(animationDuration)
         )
         )
   }
}