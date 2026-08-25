package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.collectAsStateWithLifecycleLogs
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

/**
 * Adapts the ViewModel interface to the simple state and callbacks
 * expected by the stateless PersonScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit
) {
   val tag = "<-PersonAdapter"

   // Counts successful compositions for diagnostic logging.
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect the current ViewModel state with lifecycle awareness.
   val personUiState: PersonUiState =
      viewModel.stateFlow.collectAsStateWithLifecycleLogs(tag)

   // Collect one-time effects and forward them to simple callbacks.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
      }
   }

   Column(
      modifier = modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .imePadding()
   ) {
      TopAppBar(
         windowInsets = WindowInsets(0),
         title = {
            Text(
               text = if (personUiState.isNew) stringResource(R.string.person_create)
                      else stringResource(R.string.person_detail)
            )
         },
      )

      // Show a loading indicator while the person data is being loaded.
      if (personUiState.isLoading) {
         Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      }
      else {
         val person = personUiState.person

         // Map ViewModel state to simple screen parameters and
         // map screen callbacks back to MVI intents.
         PersonScreen(
            firstName = person.firstName,
            onFirstNameChange = { viewModel.onIntent(PersonIntent.FirstNameChange(it)) },

            lastName = person.lastName,
            onLastNameChange = { viewModel.onIntent(PersonIntent.LastNameChange(it)) },

            email = person.email,
            onEmailChange = { viewModel.onIntent(PersonIntent.EmailChange(it)) },

            phone = person.phone,
            onPhoneChange = { viewModel.onIntent(PersonIntent.PhoneChange(it)) },

            imagePath = person.imagePath,

            onSave = { viewModel.onIntent(PersonIntent.Save) },
            onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

            modifier = Modifier
               .padding(horizontal = 16.dp)
               .fillMaxWidth(),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der PersonAdapter bildet die Schnittstelle zwischen ViewModel und UI.
 *   Er beobachtet den dauerhaften PersonUiState und sammelt zusätzlich die
 *   einmaligen PersonEffects.
 *
 * - Der generische EffectHandler übernimmt nur das technische Sammeln des
 *   Effect-Flow. Der PersonAdapter kennt die konkreten PersonEffects und
 *   übersetzt sie in einfache Funktionen:
 *
 *      ShowMessage  -> onMessage()
 *      ShowError    -> onError()
 *      NavigateBack -> onBack()
 *
 * - Der Adapter kennt weder SnackbarHostState noch SnackbarDuration. Wie eine
 *   Meldung dargestellt wird, entscheidet die aufrufende UI.
 *
 * - Navigation bleibt bereits in der Schnittstelle vorbereitet. Die in
 *   MainActivity übergebenen Funktionen haben in A3_02 aber noch keine
 *   Navigationsfunktion.
 *
 * - Der PersonScreen bleibt zustandslos. State fließt vom ViewModel zum Screen,
 *   Benutzeraktionen fließen als Intents zurück zum ViewModel.
 *
 * Lernziele:
 *
 * - State und einmalige Effects getrennt verarbeiten.
 * - Feature-spezifische Effects in einfache Callback-Funktionen übersetzen.
 * - Funktionen als Parameter zur Entkopplung von UI-Schichten verwenden.
 * - Generische Effect-Infrastruktur aus Shared_01 wiederverwenden.
 */
