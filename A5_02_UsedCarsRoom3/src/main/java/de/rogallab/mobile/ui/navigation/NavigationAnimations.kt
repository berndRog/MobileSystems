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

// Provides deliberately slow transitions for visualizing Navigation 3.
//
// Forward navigation uses a horizontal push. Reverse navigation distinguishes
// Save from Cancel so students can see that both operations pop the same back
// stack while representing different user intentions.
object NavigationAnimations {

   // Pushes the new destination in from the right and the current destination
   // out to the left.
   val enterTransitionSpec:
      AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      slideInHorizontally(
         initialOffsetX = { fullWidth ->
            fullWidth
         },
         animationSpec = tween(animationDuration),
      ) togetherWith
         slideOutHorizontally(
            targetOffsetX = { fullWidth ->
               -fullWidth
            },
            animationSpec = tween(animationDuration),
         )
   }

   // Selects a reverse transition according to the operation that caused pop.
   fun popTransitionSpec(
      popReason: PopReason,
   ): AnimatedContentTransitionScope<*>.() -> ContentTransform = {
      when (popReason) {
         PopReason.SAVE -> {
            // Save follows the reverse horizontal direction of the push.
            slideInHorizontally(
               initialOffsetX = { fullWidth ->
                  -fullWidth
               },
               animationSpec = tween(animationDuration),
            ) togetherWith
               slideOutHorizontally(
                  targetOffsetX = { fullWidth ->
                     fullWidth
                  },
                  animationSpec = tween(animationDuration),
               )
         }

         PopReason.CANCEL -> {
            // Cancel moves the abandoned editor down while the previous
            // destination fades back in.
            fadeIn(
               animationSpec = tween(animationDuration),
            ) togetherWith
               (
                  slideOutVertically(
                     targetOffsetY = { fullHeight ->
                        fullHeight
                     },
                     animationSpec = tween(animationDuration),
                  ) + fadeOut(
                     animationSpec = tween(animationDuration),
                  )
               )
         }
      }
   }

   // Predictive Back uses scale and opacity so gesture progress is easy to
   // observe on a physical device.
   val predictivePopTransitionSpec:
      AnimatedContentTransitionScope<*>.(Int) -> ContentTransform = { _ ->
      (
         scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(animationDuration),
         ) + fadeIn(
            animationSpec = tween(animationDuration),
         )
      ) togetherWith
         (
            scaleOut(
               targetScale = 0.85f,
               animationSpec = tween(animationDuration),
            ) + fadeOut(
               animationSpec = tween(animationDuration),
            )
         )
   }
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - Die bewusst lange Animationsdauer macht Push und Pop gut sichtbar.
// - Speichern und Abbrechen entfernen denselben Navigationseintrag, verwenden
//   aber unterschiedliche Animationen zur Verdeutlichung der Benutzerabsicht.
// - Predictive Back zeigt den interaktiven Gestenfortschritt durch Skalierung
//   und Transparenz.
