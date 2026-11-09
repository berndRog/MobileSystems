package de.rogallab.mobile.ui.navigation.comp

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import de.rogallab.mobile.ui.navigation.PeopleKey
import de.rogallab.mobile.ui.navigation.PersonKey
import de.rogallab.mobile.ui.navigation.PopReason
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun appNavEntryProvider(
   backStack: List<NavKey>,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onConfirmRemove: (String, String, () -> Unit) -> Unit,
   onNavigateBack: (PopReason) -> Unit,
   onNavigateTo: (PersonKey) -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider<NavKey> {
   // Root destination: list of people.
   entry<PeopleKey> { peopleKey ->
      val peopleViewModel = koinViewModel<PeopleViewModel>()

      PeopleAdapter(
         viewModel = peopleViewModel,

         onMessage = onMessage,
         onError = onError,

         onConfirmRemove = { message, actionLabel, personId ->
            onConfirmRemove(message, actionLabel) {
               peopleViewModel.onIntent(PeopleIntent.ConfirmRemove(personId))
            }
         },

         // null -> create, id -> detail/edit.
         onNavigateTo = { personId ->
            if (backStack.lastOrNull() == peopleKey) {
               onNavigateTo(PersonKey(personId))
            }
         },
      )
   }

   // Shared destination for create and edit.
   entry<PersonKey> { personKey ->
      val personViewModel = koinViewModel<PersonViewModel> {
         parametersOf(personKey.personId)
      }

      PersonAdapter(
         viewModel = personViewModel,

         onMessage = onMessage,
         onError = onError,

         onNavigateBack = { reason ->
            if (backStack.lastOrNull() === personKey) {
               onNavigateBack(when (reason) {
                  BackReason.Save -> PopReason.SAVE
                  BackReason.Cancel -> PopReason.Cancel
               })
            }
         },
      )
   }
}
