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
   val swipeState = rememberSwipeToDismissBoxState()
   val coroutineScope = rememberCoroutineScope()

   // Keep the dismiss handler stable across recompositions. The latest callbacks
   // are read through rememberUpdatedState without replacing the handler itself.
   val onDetailState = rememberUpdatedState(onDetail)
   val onDeleteState = rememberUpdatedState(onDelete)

   val onDismiss = remember<(SwipeToDismissBoxValue) -> Unit>(
      swipeState,
      coroutineScope,
   ) {
      { direction: SwipeToDismissBoxValue ->
         coroutineScope.launch {
            // Reset the Material state before navigation or a list change
            // modifies the surrounding composition.
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)

            when (direction) {
               SwipeToDismissBoxValue.StartToEnd -> onDetailState.value()
               SwipeToDismissBoxValue.EndToStart -> onDeleteState.value()
               SwipeToDismissBoxValue.Settled -> Unit
            }
         }
      }
   }

   SwipeToDismissBox(
      state = swipeState,
      modifier = modifier,
      enableDismissFromStartToEnd = true,
      enableDismissFromEndToStart = true,
      backgroundContent = {
         val backgroundColor by animateColorAsState(
            targetValue =
               when (swipeState.dismissDirection) {
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
            when (swipeState.dismissDirection) {
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

               SwipeToDismissBoxValue.Settled -> Unit
            }
         }
      },
      onDismiss = onDismiss,
   ) {
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
