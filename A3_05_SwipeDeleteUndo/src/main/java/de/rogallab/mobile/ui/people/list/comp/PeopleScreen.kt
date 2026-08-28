package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.SwipeCard
import de.rogallab.mobile.shared.ui.lists.ScrollToItemIfNotVisible

@Composable
fun PeopleScreen(
   people: List<Person>,
   restoredPersonId: String?,
   onRestoreHandled: () -> Unit,
   onDetail: (String) -> Unit,
   onDelete: (String) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PeopleScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val listState = rememberLazyListState()
   val detailContentDescription = stringResource(R.string.person_detail)
   val deleteContentDescription = stringResource(R.string.action_delete)

   // Undo may restore an item just outside the current viewport. Scroll only
   // when necessary and acknowledge the one-time restore target afterwards.
   ScrollToItemIfNotVisible(
      listState = listState,
      targetKey = restoredPersonId,
      items = people,
      keyOf = { person: Person -> person.id },
      onHandled = onRestoreHandled,
   )

   LazyColumn(
      state = listState,
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      items(
         items = people,
         key = { person: Person -> person.id },
      ) { person ->

         SwipeCard(
            onDetail = { onDetail(person.id) },
            onDelete = { onDelete(person.id) },
            detailContentDescription = detailContentDescription,
            deleteContentDescription = deleteContentDescription,
            modifier = Modifier.animateItem(),
         ) {
            PersonCard(
               firstName = person.firstName,
               lastName = person.lastName,
               email = person.email,
               phone = person.phone,
               imagePath = person.imagePath,
               onDetail = { onDetail(person.id) },
            )
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die LazyColumn verwendet stabile Person-IDs als Keys und einen eigenen
 *   LazyListState. Dadurch kann die aktuelle Scrollposition beobachtet und bei
 *   Bedarf gezielt verändert werden.
 *
 * - Die gemeinsame SwipeCard aus Shared kapselt die Gestenerkennung. Die
 *   konkrete PersonCard wird als content-Lambda übergeben.
 *
 *      StartToEnd -> Detail einer bestehenden Person
 *      EndToStart -> visuelles Löschen
 *
 * - Ein Tap auf PersonCard und Swipe StartToEnd verwenden denselben onDetail-
 *   Callback. Create bleibt davon getrennt und wird über den FAB ausgelöst.
 *
 * - Modifier.animateItem() animiert das Entfernen, Wiedereinfügen und
 *   Verschieben der Listeneinträge, wenn sich die sichtbare People-Liste ändert.
 *
 * - Nach Undo kann ein wieder eingefügtes Element außerhalb des Viewports
 *   liegen, insbesondere am oberen oder unteren Rand der Liste. Die generische
 *   Funktion ScrollToItemIfNotVisible prüft deshalb restoredPersonId und scrollt
 *   nur dann, wenn das Element aktuell nicht sichtbar ist.
 *
 * - Nach der Verarbeitung bestätigt onRestoreHandled den einmaligen Auftrag.
 *   Der ViewModel kann restoredPersonId anschließend wieder auf null setzen.
 *
 * Lernziele:
 *
 * - Gemeinsame Gestenkomponenten über content-Lambdas wiederverwenden.
 * - Listenänderungen mit stabilen Keys animieren.
 * - LazyListState zur gezielten Sichtbarmachung eines Elements verwenden.
 * - Einen einmaligen UI-Auftrag über State und Acknowledge verarbeiten.
 */
