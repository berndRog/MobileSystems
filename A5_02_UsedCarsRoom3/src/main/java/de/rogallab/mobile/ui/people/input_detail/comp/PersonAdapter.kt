package de.rogallab.mobile.ui.people.input_detail.comp

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.common.UiText
import de.rogallab.mobile.ui.people.input_detail.PersonEffect
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import org.koin.compose.koinInject

private const val TAG = "<-PersonAdapter"

@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   contentPadding: PaddingValues,
   onBack: () -> Unit,
   onSave: (Person, Boolean) -> Unit,
   onMessage: (UiText) -> Unit,
   validator: PersonValidator = koinInject(),
) {
   var cCount by remember { mutableIntStateOf(0) }
   SideEffect { AppLogger.compose(TAG, "Composition #${cCount++}") }

   val personUiState by viewModel.state.collectAsStateWithLifecycle()

   LaunchedEffect(viewModel) {
      viewModel.effects.collect { personEvent ->
         when (personEvent) {
            PersonEffect.NavigateBack -> onBack()
            is PersonEffect.RequestSave -> onSave(
               personEvent.person,
               personEvent.isNew,
            )
            is PersonEffect.ShowSnackbar -> onMessage(personEvent.message)
         }
      }
   }

   PersonScreen(
      personUiState = personUiState,
      validator = validator,
      contentPadding = contentPadding,
      onIntent = viewModel::onIntent,
   )
}
