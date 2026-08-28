package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun PeopleScreen(
   people: List<Person>,
   onDetail: (String) -> Unit,
   onEdit: (String) -> Unit,
   onDelete: (String) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PeopleScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   LazyColumn(
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
 * - A3_04 ersetzt die normale PersonCard in der LazyColumn durch
 *   SwipePersonCard. Der Screen erhält dadurch zwei zusätzliche Aktionen:
 *
 *      StartToEnd -> onEdit
 *      EndToStart -> onDelete
 *
 * - Die LazyColumn verwendet stabile Person-IDs als Keys. Dadurch kann Compose
 *   Listeneinträge auch nach Änderungen der Repository-Liste eindeutig
 *   zuordnen.
 *
 * - Modifier.animateItem() animiert das Entfernen und Verschieben der
 *   Listeneinträge, wenn observeAll() nach einem erfolgreichen Delete eine neue
 *   Liste liefert.
 *
 * - PeopleScreen bleibt stateless bezüglich der fachlichen Daten. Er erkennt
 *   weder Repository noch Navigation und reicht nur die resultierenden Gesten
 *   über Callback-Funktionen nach außen weiter.
 *
 * - Undo und das gezielte Scrollen zu einem wiederhergestellten Eintrag sind
 *   noch nicht Bestandteil dieses Schritts. Sie folgen erst in A3_05.
 *
 * Lernziele:
 *
 * - Swipe-Gesten in einem wiederverwendbaren Listenelement kapseln.
 * - Stabile Keys und animateItem() bei dynamischen LazyColumn-Inhalten nutzen.
 * - UI-Geste und fachliche Aktion über Callback-Funktionen entkoppeln.
 */
