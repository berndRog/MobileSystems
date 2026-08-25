package de.rogallab.mobile.ui.people.list.comp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleUiState
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable  // MVI pattern
fun PeopleAdapter(
   viewModel: PeopleViewModel = koinActivityViewModel<PeopleViewModel>(),
   modifier: Modifier = Modifier
) {
   val tag = "<-PeopleAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   val state: PeopleUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   if (state.isLoading) {
      SideEffect {Alog.d(tag, "Loading ...") }
   }
   else {
      val people = state.people
      Alog.d(tag, "observed: ${state.people.size} people")

      PeopleScreen(
         people = people,
         onDetail = { person ->
            Alog.d(tag, "Details clicked: ${person.fullName}")
            viewModel.onIntent(PeopleIntent.OpenDetail(person))
         },
         onRemove = { person ->
            Alog.d(tag, "Delete clicked: ${person.fullName}")
            viewModel.onIntent(PeopleIntent.Remove(person))
         },
         modifier = modifier
      )
   }
}