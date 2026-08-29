package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun PeopleScreen(
   people: List<Person>,
   onDetail: (String) -> Unit,
   onDelete: (String) -> Unit,
   modifier: Modifier = Modifier,
) {
   val tag = "<-PeopleScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val detailContentDescription = stringResource(R.string.person_detail)
   val deleteContentDescription = stringResource(R.string.action_delete)

   LazyColumn(
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
               imagePath = person.imagePath
            )
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A3_04 ergänzt die PersonCard in der LazyColumn um die gemeinsame SwipeCard
 *   aus Shared. Die konkrete PersonCard wird als content-Lambda übergeben.
 *
 *      StartToEnd -> onDetail
 *      EndToStart -> onDelete
 *
 * - Ein Tap auf PersonCard und ein Swipe von StartToEnd öffnen beide denselben
 *   PersonScreen im Modus Detail. Das Anlegen einer neuen Person bleibt davon
 *   getrennt und wird ausschließlich über den FAB ausgelöst.
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
 * - Eine gemeinsame Swipe-Komponente mit einem fachlichen content-Lambda nutzen.
 * - Stabile Keys und animateItem() bei dynamischen LazyColumn-Inhalten einsetzen.
 * - UI-Geste und fachliche Aktion über Callback-Funktionen entkoppeln.
 */
