package de.rogallab.mobile.ui.navigation.comp

import androidx.compose.material3.SnackbarHostState
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
   snackbarHostState: SnackbarHostState,
   onConfirmRemove: (String, String, () -> Unit) -> Unit,
   onNavigateBack: (PopReason) -> Unit,
   onNavigateTo: (PersonKey) -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider<NavKey> {
   // Root destination: list of people.
   entry<PeopleKey> {
      val peopleViewModel = koinViewModel<PeopleViewModel>()

      PeopleAdapter(
         viewModel = peopleViewModel,
         snackbarHostState = snackbarHostState,

         onMessage = onMessage,
         onError = onError,

         // A swipe delete requests confirmation first. Only selecting
         // the Snackbar action triggers the repository deletion.
         onConfirmRemove = { message, actionLabel, personId ->
            onConfirmRemove(message, actionLabel) {
               peopleViewModel.onIntent(PeopleIntent.ConfirmRemove(personId))
            }
         },

         onNavigateBack = {
            onNavigateBack(PopReason.Cancel)
         },

         // null -> create, id -> detail/edit.
         onNavigateTo = { personId ->
            onNavigateTo(PersonKey(personId))
         },
      )
   }

   // Shared destination for create and edit. The new part of A4_01 is
   // gallery/camera image selection and the image edit lifecycle.
   entry<PersonKey> { personKey ->
      val personViewModel = koinViewModel<PersonViewModel> {
         parametersOf(personKey.personId)
      }

      PersonAdapter(
         viewModel = personViewModel,
         snackbarHostState = snackbarHostState,

         // showMessage() starts its coroutine in this navigation-level
         // controller before NavigateBack removes the Person destination.
         onMessage = onMessage,
         onError = onError,

         onNavigateBack = { reason ->
            onNavigateBack(when (reason) {
               BackReason.Save -> PopReason.Save
               BackReason.Cancel -> PopReason.Cancel
            })
         },
      )
   }
}
