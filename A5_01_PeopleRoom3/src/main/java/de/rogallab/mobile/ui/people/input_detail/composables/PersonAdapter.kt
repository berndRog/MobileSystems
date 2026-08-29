package de.rogallab.mobile.ui.people.input_detail.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Person as PersonEntity
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.people.input_detail.PersonEvent
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import org.koin.compose.koinInject

// Stateful adapter for both workflows:
//
// - create a new person,
// - load and edit an existing person.
//
// PeopleNavigation creates a route-scoped PersonViewModel. Consequently every
// PersonKey on the back stack owns an independent ViewModel instance although
// both workflows use the same ViewModel and the same screen classes.
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onSave: (PersonEntity, Boolean) -> Unit,
   onMessage: (UiText) -> Unit,
   validator: PersonValidator = koinInject(),
) {
   val state by viewModel.state.collectAsStateWithLifecycle()

   // Collects one-time actions emitted by the route-scoped PersonViewModel.
   LaunchedEffect(viewModel) {
      viewModel.events.collect { personEvent ->
         when (personEvent) {
            PersonEvent.NavigateBack -> onBack()

            is PersonEvent.RequestSave -> {
               onSave(
                  personEvent.person,
                  personEvent.isNew,
               )
            }

            is PersonEvent.ShowSnackbar -> {
               onMessage(personEvent.message)
            }
         }
      }
   }

   PersonScreen(
      state = state,
      validator = validator,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
