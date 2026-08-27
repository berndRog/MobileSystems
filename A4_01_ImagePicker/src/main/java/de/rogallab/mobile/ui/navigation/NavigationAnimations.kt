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
import de.rogallab.mobile.Globals.animationDuration

object NavigationAnimations {

   // Forward navigation:
   // - The new screen enters from the right.
   // - The current screen exits to the left.
   val enterTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      slideInHorizontally(
         initialOffsetX = { fullWidth -> fullWidth },
         animationSpec = tween(animationDuration),
      ) togetherWith slideOutHorizontally(
         targetOffsetX = { fullWidth -> -fullWidth },
         animationSpec = tween(animationDuration),
      )
   }

   // Back navigation uses a different animation for Save and Cancel.
   fun popTransitionSpec(
      popReason: PopReason,
   ): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      when (popReason) {
         PopReason.Save -> {
            slideInHorizontally(
               initialOffsetX = { fullWidth -> -fullWidth },
               animationSpec = tween(animationDuration),
            ) togetherWith slideOutHorizontally(
               targetOffsetX = { fullWidth -> fullWidth },
               animationSpec = tween(animationDuration),
            )
         }

         PopReason.Cancel -> {
            fadeIn(
               animationSpec = tween(animationDuration)
            ) togetherWith (
               slideOutVertically(
                  targetOffsetY = { fullHeight -> fullHeight },
                  animationSpec = tween(animationDuration),
               ) + fadeOut(
                  animationSpec = tween(animationDuration)
               )
            )
         }
      }
   }

   // Predictive back uses scale and fade to make the gesture visible.
   val predictivePopTransitionSpec: AnimatedContentTransitionScope<*>.(Int) -> ContentTransform = { _ ->
      (
         scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(animationDuration),
         ) + fadeIn(
            animationSpec = tween(animationDuration),
         )
      ) togetherWith (
         scaleOut(
            targetScale = 0.85f,
            animationSpec = tween(animationDuration),
         ) + fadeOut(
            animationSpec = tween(animationDuration),
         )
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Vorwärts- und Rückwärtsnavigation erhalten bewusst unterschiedliche
 *   Animationen. Dadurch ist die Richtung einer Navigation sofort sichtbar.
 *
 * - Auch Save und Cancel unterscheiden sich beim Zurücknavigieren:
 *   Save verwendet die klassische horizontale Rückwärtsanimation, Cancel
 *   blendet den vorherigen Screen ein und lässt den aktuellen nach unten
 *   verschwinden.
 *
 * - Predictive Back besitzt eine eigene Scale-/Fade-Animation.
 *
 * Lernziele:
 *
 * - Navigationsrichtung über Animationen sichtbar machen.
 * - transitionSpec, popTransitionSpec und predictivePopTransitionSpec
 *   unterscheiden.
 */
