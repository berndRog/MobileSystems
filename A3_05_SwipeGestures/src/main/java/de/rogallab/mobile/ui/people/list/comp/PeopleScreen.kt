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
 * - Die LazyColumn verwendet stabile Person-IDs als Keys.
 *
 * - Modifier.animateItem() animiert das Entfernen, Wiedereinfügen und
 *   Verschieben der Listeneinträge, wenn sich die sichtbare People-Liste ändert.
 *
 * - SwipePersonCard kapselt die Gestenerkennung. PeopleScreen kennt nur die
 *   resultierenden Events onEdit und onDelete.
 *
 * Lernziele:
 *
 * - Gesten in wiederverwendbare Listenelemente auslagern.
 * - Listenänderungen mit stabilen Keys animieren.
 * - Visuelles Löschen und Wiedereinfügen über State-Änderungen darstellen.
 */
