package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.composables.SwipeEditDeleteItem
import de.rogallab.mobile.ui.people.list.PeopleIntent

@Composable
fun PeopleLazyList(
   people: List<Person>,
   lazyListState: LazyListState,
   onIntent: (PeopleIntent) -> Unit,
) {
   val tag = "<-PeopleLazyList"
   var nComp by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(tag, "Composition #${nComp++}") }

   LazyColumn(
      state = lazyListState,
      contentPadding = PaddingValues(
         start = 12.dp,
         end = 12.dp,
         bottom = 96.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
   ) {
      itemsIndexed(
         items = people,
         key = { _, person -> person.id },
      ) { originalIndex, person ->
         SwipeEditDeleteItem(
            itemKey = person.id,
            editContentDescription = R.string.accessibility_edit_person,
            deleteContentDescription = R.string.accessibility_delete_person,
            onEdit = { onIntent(PeopleIntent.Open(person.id)) },
            onRemove = {
               onIntent(
                  PeopleIntent.Remove(
                     person = person,
                     originalIndex = originalIndex,
                  )
               )
            }
         ) {
            AppLogger.verbose("<-PeopleLazyList", "PersonCard: ${person.firstName} ${person.lastName}")
            PersonCard(
               person = person
            )
         }
      }
   }
}