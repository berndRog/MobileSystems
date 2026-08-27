package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import kotlinx.coroutines.launch

@Composable
fun SwipePersonCard(
   firstName: String,
   lastName: String,
   email: String?,
   phone: String?,
   imagePath: String?,
   onDetail: () -> Unit,
   onEdit: () -> Unit,
   onDelete: () -> Unit,
   modifier: Modifier = Modifier,
) {
   val swipeState = rememberSwipeToDismissBoxState()
   val coroutineScope = rememberCoroutineScope()

   SwipeToDismissBox(
      state = swipeState,
      modifier = modifier,
      enableDismissFromStartToEnd = true,
      enableDismissFromEndToStart = true,
      backgroundContent = {
         val backgroundColor by animateColorAsState(
            targetValue =
               when (swipeState.targetValue) {
                  SwipeToDismissBoxValue.StartToEnd ->
                     MaterialTheme.colorScheme.primaryContainer

                  SwipeToDismissBoxValue.EndToStart ->
                     MaterialTheme.colorScheme.errorContainer

                  SwipeToDismissBoxValue.Settled ->
                     MaterialTheme.colorScheme.surfaceVariant
               },
            label = "swipeBackgroundColor",
         )

         Box(
            modifier = Modifier
               .fillMaxSize()
               .background(backgroundColor),
         ) {
            when (swipeState.targetValue) {
               SwipeToDismissBoxValue.StartToEnd ->
                  Icon(
                     imageVector = Icons.Default.Edit,
                     contentDescription = stringResource(R.string.action_edit),
                     modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.CenterStart)
                        .padding(horizontal = 20.dp),
                     tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )

               SwipeToDismissBoxValue.EndToStart ->
                  Icon(
                     imageVector = Icons.Default.Delete,
                     contentDescription = stringResource(R.string.action_delete),
                     modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.CenterEnd)
                        .padding(horizontal = 20.dp),
                     tint = MaterialTheme.colorScheme.onErrorContainer,
                  )

               SwipeToDismissBoxValue.Settled -> Unit
            }
         }
      },
      onDismiss = { direction ->
         when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> {
               // Editing navigates away. Reset the swipe state so the card is
               // settled again when the list becomes visible later.
               onEdit()
               coroutineScope.launch {
                  swipeState.reset()
               }
            }

            SwipeToDismissBoxValue.EndToStart ->
               onDelete()

            SwipeToDismissBoxValue.Settled -> Unit
         }
      },
   ) {
      PersonCard(
         firstName = firstName,
         lastName = lastName,
         email = email,
         phone = phone,
         imagePath = imagePath,
         onDetail = onDetail,
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - SwipeToDismissBox wertet die horizontale Wischrichtung aus:
 *
 *      StartToEnd -> Bearbeiten
 *      EndToStart -> visuelles Löschen
 *
 * - backgroundContent macht die beiden möglichen Aktionen bereits während
 *   der Geste sichtbar. Farbe und Symbol ändern sich abhängig von targetValue.
 *
 * - Beim Editieren wird der Swipe-State zurückgesetzt, weil die Card nicht aus
 *   der Liste entfernt wird. Beim Löschen übernimmt dagegen PeopleViewModel
 *   das visuelle Entfernen aus dem State.
 *
 * Lernziele:
 *
 * - SwipeToDismissBox für zwei unterschiedliche Gestenrichtungen verwenden.
 * - Geste und fachliche Aktion über Callback-Funktionen trennen.
 * - Animiertes visuelles Feedback während einer Geste einsetzen.
 */
