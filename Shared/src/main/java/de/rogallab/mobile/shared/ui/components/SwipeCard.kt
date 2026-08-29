package de.rogallab.mobile.shared.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SwipeCard(
   onDetail: () -> Unit,
   onDelete: () -> Unit,
   detailContentDescription: String,
   deleteContentDescription: String,
   modifier: Modifier = Modifier,
   content: @Composable () -> Unit,
) {
   // Holds the current Material swipe state of the card.
   val swipeState = rememberSwipeToDismissBoxState()

   // Used to reset the swipe state before executing the resulting action.
   val coroutineScope = rememberCoroutineScope()

   // Keep the latest callbacks without recreating the dismiss handler
   // when the surrounding composition changes.
   val onDetailState = rememberUpdatedState(onDetail)
   val onDeleteState = rememberUpdatedState(onDelete)

   // Keep the dismiss handler stable across recompositions.
   val onDismiss = remember<(SwipeToDismissBoxValue) -> Unit>(
      swipeState,
      coroutineScope,
   ) {
      { direction: SwipeToDismissBoxValue ->

         coroutineScope.launch {
            // Reset the Material swipe state before navigation or a list change
            // modifies the surrounding composition.
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)

            // Map the completed swipe direction to the corresponding action.
            when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> onDetailState.value() // onDetail() is called
               SwipeToDismissBoxValue.EndToStart -> onDeleteState.value() // onDelete() is called
               SwipeToDismissBoxValue.Settled -> Unit
            }
         }
      }
   }

   // Wrap the supplied card content with horizontal swipe gestures.
   SwipeToDismissBox(
      state = swipeState,
      modifier = modifier,
      enableDismissFromStartToEnd = true,
      enableDismissFromEndToStart = true,

      // Draw the action background behind the moving card.
      backgroundContent = {

         // Change the background immediately when the swipe direction changes.
         val backgroundColor by animateColorAsState(
            targetValue =
               when (swipeState.dismissDirection) {
                  SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                  SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                  SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surfaceVariant
               },
            label = "swipeBackgroundColor",
         )

         Box(
            modifier = Modifier
               .fillMaxSize()
               .background(backgroundColor),
         ) {

            // Show the action icon on the side revealed by the swipe.
            when (swipeState.dismissDirection) {

               // StartToEnd opens the detail screen of the existing item.
               SwipeToDismissBoxValue.StartToEnd ->
                  Icon(
                     imageVector = Icons.Default.Edit,
                     contentDescription = detailContentDescription,
                     modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.CenterStart)
                        .padding(horizontal = 20.dp)
                        .size(36.dp),
                     tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )

               // EndToStart requests deletion of the existing item.
               SwipeToDismissBoxValue.EndToStart ->
                  Icon(
                     imageVector = Icons.Default.Delete,
                     contentDescription = deleteContentDescription,
                     modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.CenterEnd)
                        .padding(horizontal = 20.dp)
                        .size(36.dp),
                     tint = MaterialTheme.colorScheme.onErrorContainer,
                  )

               // No action indicator is shown while the card is settled.
               SwipeToDismissBoxValue.Settled ->
                  Unit
            }
         }
      },

      // Handle the action after Material has completed the swipe.
      onDismiss = onDismiss,
   ) {
      // The concrete card is supplied by the caller.
      // SwipeCard therefore remains independent of Person, Car, or other entities.
      content()
   }
}
/*
 * Didaktik und Lernziele
 *
 * - SwipeCard kapselt die wiederverwendbare Swipe-Mechanik und kennt keine
 *   fachliche Entity wie Person oder Car. Der konkrete Karteninhalt wird über
 *   den letzten content-Lambda übergeben.
 *
 * - Die beiden Wischrichtungen haben im Kurs eine feste Bedeutung:
 *
 *      StartToEnd -> Detail einer bestehenden Entity öffnen
 *      EndToStart -> Entity löschen
 *
 * - Das Edit-Symbol bleibt als visuelles Zeichen für den bearbeitbaren
 *   Detail-Screen erhalten. Die fachliche Aktion heißt trotzdem onDetail.
 *
 * - Farbe und Symbol richten sich nach dismissDirection. Dadurch erscheint das
 *   visuelle Feedback bereits beim Beginn der Wischbewegung und nicht erst beim
 *   Erreichen eines späteren targetValue.
 *
 * - onDismiss wird mit remember(...) stabil gehalten. rememberUpdatedState
 *   liefert trotzdem immer die aktuellen onDetail- und onDelete-Callbacks.
 *   Das verhindert insbesondere nach Undo und Recomposition das unbeabsichtigte
 *   erneute Auslösen eines bereits abgeschlossenen Swipe-Vorgangs.
 *
 * - Vor onDetail() oder onDelete() wird SwipeToDismissBoxState mit
 *   snapTo(Settled) zurückgesetzt. Erst danach dürfen Navigation oder eine
 *   Listenänderung die umgebende Composition verändern.
 *
 * - Die Content-Descriptions werden vom Aufrufer übergeben. Shared bleibt damit
 *   unabhängig von den String-Ressourcen eines konkreten Beispielmoduls.
 *
 * Lernziele:
 *
 * - Eine UI-Komponente über einen content-Lambda fachlich entkoppeln.
 * - Aktuelle Gestenrichtung und endgültige Swipe-Aktion unterscheiden.
 * - Stabilen Callback-Zustand bei Recomposition und Undo sicherstellen.
 */
