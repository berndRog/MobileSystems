package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.collectAsStateWithLifecycleLogs
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect the current ViewModel state with lifecycle awareness.
   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   // Collect one-time effects and translate them into UI callbacks.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   val person = personUiState.person

   PersonScreen(
      isNew = personUiState.isNew,
      isLoading = personUiState.isLoading,

      firstName = person.firstName,
      onFirstNameChange = { viewModel.onIntent(PersonIntent.FirstNameChange(it)) },

      lastName = person.lastName,
      onLastNameChange = { viewModel.onIntent(PersonIntent.LastNameChange(it)) },

      email = person.email,
      onEmailChange = { viewModel.onIntent(PersonIntent.EmailChange(it)) },

      phone = person.phone,
      onPhoneChange = { viewModel.onIntent(PersonIntent.PhoneChange(it)) },

      imagePath = person.imagePath,

      onBack = { viewModel.onIntent(PersonIntent.Cancel) },

      onSave = { viewModel.onIntent(PersonIntent.Save) },
      onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

      modifier = modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .imePadding()
         .fillMaxWidth(),
   )
}

/*
 * Didaktik und Lernziele
 *
 * - Der Adapter verarbeitet zwei Datenrichtungen unabhängig voneinander:
 *   State zum Screen und einmalige Effects zur übergeordneten UI.
 *
 * - ShowMessage und ShowError werden an den navigationweit lebenden
 *   SnackbarController weitergereicht. NavigateBack wird dagegen in eine
 *   Back-Stack-Operation übersetzt.
 *
 * - Der Back-Pfeil und der Cancel-Button erzeugen beide PersonIntent.Cancel.
 *   Erst das ViewModel entscheidet daraus NavigateBack(BackReason.Cancel).
 *
 * Lernziele:
 *
 * - State, Intent und Effect in einem Adapter verbinden.
 * - Meldungen und Navigation als getrennte Effect-Arten behandeln.
 */
