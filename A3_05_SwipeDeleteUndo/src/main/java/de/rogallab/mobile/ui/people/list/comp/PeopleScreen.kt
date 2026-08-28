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
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.lists.ScrollToItemIfNotVisible

@Composable
fun PeopleScreen(
   people: List<Person>,
   restoredPersonId: String?,
   onRestoreHandled: () -> Unit,
   onDetail: (String) -> Unit,
   onEdit: (String) -> Unit,
   onDelete: (String) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PeopleScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val listState = rememberLazyListState()

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

         SwipePersonCard(
            firstName = person.firstName,
            lastName = person.lastName,
            email = person.email,
            phone = person.phone,
            imagePath = person.imagePath,
            onDetail = { onDetail(person.id) },
            onEdit = { onEdit(person.id) },
            onDelete = { onDelete(person.id) },
            modifier = Modifier.animateItem(),
         )
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
 * - SwipePersonCard kapselt weiterhin die Gestenerkennung. PeopleScreen kennt
 *   nur die resultierenden Events onEdit und onDelete.
 *
 * Lernziele:
 *
 * - Gesten in wiederverwendbare Listenelemente auslagern.
 * - Listenänderungen mit stabilen Keys animieren.
 * - LazyListState zur gezielten Sichtbarmachung eines Elements verwenden.
 * - Einen einmaligen UI-Auftrag über State und Acknowledge verarbeiten.
 */
