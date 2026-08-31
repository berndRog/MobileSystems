package de.rogallab.mobile.ui.features.article.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.Globals.animationDuration
import de.rogallab.mobile.data.dtos.Article
import de.rogallab.mobile.domain.utilities.logComp
import de.rogallab.mobile.domain.utilities.logDebug
import kotlinx.coroutines.delay

/**
 * SwipePersonListItem — Algorithmic Overview
 *
 * PURPOSE
 *  - Displays a row that reacts to horizontal swipe gestures:
 *      • StartToEnd (left → right): triggers edit navigation
 *      • EndToStart (right → left): triggers a delete animation + Undo prompt
 *  - The gesture acts only as an *input trigger*. The component itself never
 *    remains in a visually dismissed state. Instead, it starts a controlled
 *    exit animation and defers the actual data mutation to the ViewModel.
 *
 * STATE
 *  - `isRemoved`: ephemeral UI state controlling the AnimatedVisibility exit.
 *      • Initialized with `remember(person.id)` to reset cleanly after Undo.
 *  - `SwipeToDismissBoxState`: detects swipe direction only; immediately reset
 *    to `Settled` so Compose’s internal dismiss logic never takes over.
 *
 * ALGORITHM
 *  1) User swipes → state.currentValue changes.
 *  2) If StartToEnd → call `onNavigate(person.id)` and snap back.
 *  3) If EndToStart → set `isRemoved = true` to trigger the exit animation,
 *     then snap back (we manage visuals ourselves).
 *  4) A `LaunchedEffect(isRemoved)` waits for the animation duration, then:
 *       • Calls `onDelete()` to update UI + repository via ViewModel.
 *       • Calls `onUndo()` to show the Snackbar with Undo action.
 *  5) If the same person is restored later, Compose recomposes with a new key,
 *     resetting `isRemoved = false` automatically.
 *
 * WHY IT WORKS
 *  - **Decoupled gesture & state:** prevents conflicts with internal dismiss logic.
 *  - **Predictable Undo:** keying state by person.id guarantees a fresh state.
 *  - **Smooth UX:** user gets immediate visual feedback; expensive I/O happens later.
 *
 * This pattern demonstrates an *Optimistic-then-Persist* update:
 * The UI responds instantly, while persistence catches up asynchronously.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeArticleWithoutUndoListItem(
   article: Article,
   onNavigate: (Int) -> Unit,
   onRemove: () -> Unit,
   content: @Composable () -> Unit
) {
   val tag = "<-SwipePersonLiItem"
   val compositionCount = remember { mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${compositionCount.intValue++}") }

   // Steuert nur die Sichtbarkeit / Exit-Animation
   var isRemoved by remember(article.id) { mutableStateOf(false) }

   val state = rememberSwipeToDismissBoxState(
      positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
   )

   // Swipe → Aktion
   LaunchedEffect(state.currentValue) {
      when (state.currentValue) {
         SwipeToDismissBoxValue.StartToEnd -> {
            logDebug(tag, "Swipe to Edit")
            article.id?.let { onNavigate(it) }
            state.snapTo(SwipeToDismissBoxValue.Settled)
         }
         SwipeToDismissBoxValue.EndToStart -> {
            logDebug(tag, "Swipe to Delete for ${article.id} ")
            isRemoved = true                // Start Exit-Animation
            state.snapTo(SwipeToDismissBoxValue.Settled)
         }
         SwipeToDismissBoxValue.Settled -> Unit
      }
   }

   // Sicherheitshalber zurücksetzen beim Identitätswechsel
   LaunchedEffect(article.id) {
      state.snapTo(SwipeToDismissBoxValue.Settled)
   }

   // Nach Ende der Animation endgültig entfernen (ohne Undo)
   LaunchedEffect(isRemoved, article.id) {
      if (isRemoved) {
         delay(animationDuration.toLong())
         onRemove()        // ViewModel / Liste aktualisieren
      }
   }

   AnimatedVisibility(
      visible = !isRemoved,
      exit = shrinkVertically(
         animationSpec = tween(durationMillis = animationDuration),
         shrinkTowards = Alignment.Top
      ) + fadeOut()
   ) {
      SwipeToDismissBox(
         state = state,
         backgroundContent = { SetSwipeBackground(state) },
         enableDismissFromStartToEnd = true,
         enableDismissFromEndToStart = true,
         modifier = Modifier.padding(vertical = 4.dp)
      ) {
         content()
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSwipeBackground(state: SwipeToDismissBoxState) {
   val (colorBox, colorIcon, alignment, icon, description, scale) =
      GetSwipeProperties(state)

   Box(
      Modifier
         .fillMaxSize()
         .background(
            color = colorBox,
            shape = RoundedCornerShape(10.dp)
         )
         .padding(horizontal = 16.dp),
      contentAlignment = alignment
   ) {
      Icon(
         imageVector = icon,
         contentDescription = description,
         modifier = Modifier.scale(scale),
         tint = colorIcon
      )
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetSwipeProperties(
   state: SwipeToDismissBoxState
): SwipeProperties {
   val direction = state.dismissDirection

   val colorBox: Color = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Color(0xFF008000) // Green
      SwipeToDismissBoxValue.EndToStart -> Color(0xFFB22222) // Firebrick Red
      else -> MaterialTheme.colorScheme.surface
   }
   val colorIcon: Color = Color.White

   val alignment: Alignment = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
      SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
      else -> Alignment.Center
   }

   val icon: ImageVector = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit
      SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
      else -> Icons.Outlined.Info
   }

   val description: String = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> "Edit"
      SwipeToDismissBoxValue.EndToStart -> "Delete"
      else -> "Unknown Action"
   }

   val scale = if (state.targetValue == SwipeToDismissBoxValue.Settled) 1.2f else 1.2f

   return SwipeProperties(colorBox, colorIcon, alignment, icon, description, scale)
}

data class SwipeProperties(
   val colorBox: Color,
   val colorIcon: Color,
   val alignment: Alignment,
   val icon: ImageVector,
   val description: String,
   val scale: Float
)