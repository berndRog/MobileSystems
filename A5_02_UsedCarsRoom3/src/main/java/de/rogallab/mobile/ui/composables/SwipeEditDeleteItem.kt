package de.rogallab.mobile.ui.composables

import androidx.annotation.StringRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.Globals
import kotlinx.coroutines.delay

/**
 * Common swipe container for list items.
 *
 * A swipe from start to end opens the item for editing. A swipe from end to
 * start first animates the item out of the list and then requests its removal.
 * The entity-specific wrapper maps these callbacks to its own intents.
 */
@Composable
fun SwipeEditDeleteItem(
   itemKey: Any,
   @StringRes editContentDescription: Int,
   @StringRes deleteContentDescription: Int,
   onEdit: () -> Unit,
   onRemove: () -> Unit,
   modifier: Modifier = Modifier,
   content: @Composable () -> Unit,
) {
   var isVisible by remember(itemKey) { mutableStateOf(true) }
   var removalRequested by remember(itemKey) { mutableStateOf(false) }
   val dismissState = rememberSwipeToDismissBoxState()

   LaunchedEffect(dismissState.settledValue) {
      when (dismissState.settledValue) {
         SwipeToDismissBoxValue.StartToEnd -> {
            onEdit()
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
         }

         SwipeToDismissBoxValue.EndToStart -> {
            if (!removalRequested) {
               removalRequested = true
               isVisible = false
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
         }

         SwipeToDismissBoxValue.Settled -> Unit
      }
   }

   LaunchedEffect(removalRequested) {
      if (!removalRequested) return@LaunchedEffect

      delay(Globals.animationDuration.toLong())
      onRemove()
   }

   AnimatedVisibility(
      visible = isVisible,
      exit = shrinkVertically(
         animationSpec = tween(Globals.animationDuration),
         shrinkTowards = Alignment.Top,
      ) + fadeOut(animationSpec = tween(Globals.animationDuration)),
   ) {
      SwipeToDismissBox(
         state = dismissState,
         backgroundContent = {
            SwipeEditDeleteBackground(
               dismissState = dismissState,
               editContentDescription = editContentDescription,
               deleteContentDescription = deleteContentDescription,
            )
         },
         enableDismissFromStartToEnd = true,
         enableDismissFromEndToStart = true,
         modifier = modifier.padding(vertical = 4.dp),
      ) {
         content()
      }
   }
}

@Composable
private fun SwipeEditDeleteBackground(
   dismissState: SwipeToDismissBoxState,
   @StringRes editContentDescription: Int,
   @StringRes deleteContentDescription: Int,
) {
   val direction = dismissState.dismissDirection
   val isDelete = direction == SwipeToDismissBoxValue.EndToStart
   val isEdit = direction == SwipeToDismissBoxValue.StartToEnd

   val alignment = when {
      isDelete -> Alignment.CenterEnd
      isEdit -> Alignment.CenterStart
      else -> Alignment.Center
   }
   val backgroundColor = when {
      isDelete -> MaterialTheme.colorScheme.errorContainer
      isEdit -> MaterialTheme.colorScheme.tertiaryContainer
      else -> MaterialTheme.colorScheme.surfaceContainer
   }
   val foregroundColor = when {
      isDelete -> MaterialTheme.colorScheme.onErrorContainer
      isEdit -> MaterialTheme.colorScheme.onTertiaryContainer
      else -> MaterialTheme.colorScheme.onSurface
   }

   Box(
      modifier = Modifier
         .fillMaxSize()
         .background(backgroundColor, RoundedCornerShape(12.dp))
         .padding(horizontal = 20.dp),
      contentAlignment = alignment,
   ) {
      if (isDelete || isEdit) {
         Icon(
            imageVector = if (isDelete) Icons.Outlined.Delete else Icons.Outlined.Edit,
            contentDescription = stringResource(
               if (isDelete) deleteContentDescription else editContentDescription
            ),
            tint = foregroundColor,
         )
      }
   }
}
