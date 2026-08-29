package de.rogallab.mobile.ui.people.list.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.Globals
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.compose
import de.rogallab.mobile.ui.people.list.PeopleIntent
import kotlinx.coroutines.delay

/**
 * Verarbeitet die Wischgesten einer Listenzeile.
 *
 * Von links nach rechts wird der Editor geöffnet. Von rechts nach links startet
 * zunächst eine kontrollierte Ausblendanimation. Erst danach wird
 * [de.rogallab.mobile.ui.people.list.PeopleIntent.Remove] gesendet. Das Listen-ViewModel entfernt die Person
 * nur visuell; Room wird erst nach Ablauf der Undo-Snackbar geändert.
 */
@Composable
fun SwipePeopleItem(
   person: Person,
   originalIndex: Int,
   onIntent: (PeopleIntent) -> Unit,
   content: @Composable () -> Unit
) {

   val tag = "<-SwipePeopleItem"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp.intValue++}") }


   var isVisible by remember(person.id) { mutableStateOf(true) }
   var removalRequested by remember(person.id) { mutableStateOf(false) }

   val dismissState = rememberSwipeToDismissBoxState()

   LaunchedEffect(dismissState.settledValue) {
      when (dismissState.settledValue) {
         SwipeToDismissBoxValue.StartToEnd -> {
            onIntent(
               PeopleIntent.Open(personId = person.id)
            )
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
      onIntent(
         PeopleIntent.Remove(
            person = person,
            originalIndex = originalIndex
         )
      )
   }

   AnimatedVisibility(
      visible = isVisible,
      exit = shrinkVertically(
         animationSpec = tween(Globals.animationDuration),
         shrinkTowards = Alignment.Top
      ) + fadeOut(
         animationSpec = tween(Globals.animationDuration)
      )
   ) {
      SwipeToDismissBox(
         state = dismissState,
         backgroundContent = {
            SwipeBackground(state = dismissState)
         },
         enableDismissFromStartToEnd = true,
         enableDismissFromEndToStart = true,
         modifier = Modifier.padding(vertical = 4.dp)
      ) {
         content()
      }
   }
}

@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState) {
   val direction = state.dismissDirection
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
         .background(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp)
         )
         .padding(horizontal = 20.dp),
      contentAlignment = alignment
   ) {
      if (isDelete || isEdit) {
         Icon(
            imageVector = if (isDelete) {
               Icons.Outlined.Delete
            } else {
               Icons.Outlined.Edit
            },
            contentDescription = if (isDelete) {
               stringResource(R.string.accessibility_delete_person)
            } else {
               stringResource(R.string.accessibility_edit_person)
            },
            tint = foregroundColor
         )
      }
   }
}