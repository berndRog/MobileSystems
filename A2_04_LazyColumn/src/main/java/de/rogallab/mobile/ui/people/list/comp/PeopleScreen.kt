package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.Alog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
   people: List<Person>,
   onDetail: (Person) -> Unit,
   onRemove: (Person) -> Unit,
   modifier: Modifier = Modifier
) {
   val tag = "<-PeopleScreen"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   LazyColumn(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(8.dp)
   ) {
      items(
         items = people,
         key = { it: Person -> it.id }
      ) { person ->

         Alog.d(tag, "Person: ${person.fullName}")

         PersonCard(
            firstName = person.firstName,
            lastName = person.lastName,
            onDetail = {
               Alog.d(tag, "onDetail clicked")
               onDetail(person)
            },
            onRemove = {
               Alog.d(tag, "onRemove clicked}")
               onRemove(person)
            }
         )
      }
   }
}

