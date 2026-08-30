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
 * - A4_01 verwendet die gemeinsame SwipeCard aus Shared. Die PersonCard bleibt
 *   der konkrete Inhalt der Karte.
 *
 *      StartToEnd -> Detail einer bestehenden Person
 *      EndToStart -> Löschvorgang anfordern
 *
 * - Ein Tap auf PersonCard und Swipe StartToEnd verwenden denselben onDetail-
 *   Callback. Create bleibt davon getrennt und wird über den FAB ausgelöst.
 *
 * - Die LazyColumn verwendet stabile Person-IDs als Keys. Modifier.animateItem()
 *   animiert die Listenänderung, nachdem das Repository eine bestätigte Löschung
 *   ausgeführt und observeAll() die neue Liste geliefert hat.
 *
 * - Da A4_01 kein Undo enthält, benötigt der Screen weder restoredPersonId noch
 *   einen eigenen LazyListState für das gezielte Wiederanzeigen eines Elements.
 *   Diese Erweiterung folgt in A4_02_ImagePickerUndo.
 *
 * Lernziele:
 *
 * - Gemeinsame UI-Komponenten über Modulgrenzen hinweg wiederverwenden.
 * - Stabile Keys und animateItem() bei dynamischen Listen einsetzen.
 * - Delete-Bestätigung und Undo als getrennte Ausbaustufen verstehen.
 */
