package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectHandler
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
   snackbarHostState: SnackbarHostState,
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit
) {
   val tag = "<-PersonAdapter"
   // Counts successful compositions for diagnostic logging.
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   // Collect the PersonUiState with lifecycle awareness.
   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()
   // Person data
   val person = personUiState.person
   val loadFailure = personUiState.loadFailure

   // Collect one-time effects and forward them to simple callbacks.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
      }
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),

      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(enabled = !personUiState.isLoading && !personUiState.isSaving &&
                  loadFailure == null, onClick = {
                  viewModel.onIntent(PersonIntent.Save)
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_save))
               }
            },
            title = {
               Text(text = stringResource( if (personUiState.isNew) R.string.person_create
               else R.string.person_detail ))
            },
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState,
                      modifier = Modifier.imePadding())
      },
   ) { innerPadding ->
      // Show a loading indicator if the person data is still being loaded.
      if (personUiState.isLoading) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      } else if (loadFailure != null) {
         Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Text(text = loadFailure.message)
            Button(onClick = { viewModel.onIntent(PersonIntent.RetryLoad) }) {
               Text(text = stringResource(R.string.action_retry))
            }
         }
      } else {
         // Show person data
         val person = personUiState.person

         // Map ViewModel state to simple screen parameters and
         // map screen callbacks back to MVI intents.
         PersonScreen(
            isNew = personUiState.isNew,
            isLoading = personUiState.isLoading,
            isSaving = personUiState.isSaving,
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

            modifier = modifier
               .fillMaxSize()
               .padding(innerPadding)
               .padding(horizontal = 16.dp)
               .verticalScroll(rememberScrollState())
               .imePadding(),
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
 *
 * - Ein lokaler SnackbarHost zeigt Meldungen des ausgewählten Screens.
 *   MainActivity übergibt dafür einen SnackbarHostState und Callbacks.
 *
 * - A3_02 besitzt noch keine Navigation. Der Toolbar-Pfeil demonstriert
 *   hier ausschließlich den Save-Intent.
 *
 * - Der PersonScreen bleibt zustandslos. State fließt vom ViewModel zum Screen,
 *   Benutzeraktionen fließen als Intents zurück zum ViewModel.
 * - Ladefehler stehen dauerhaft im State; ShowMessage und ShowError bleiben
 *   einmalige Effects. NotFound und Failed bieten ohne Backstack Retry.
 *   Während Save sind weitere Aktionen gesperrt.
 *
 * Lernziele:
 *
 * - State und einmalige Effects getrennt verarbeiten.
 * - Feature-spezifische Effects in einfache Callback-Funktionen übersetzen.
 * - Funktionen als Parameter zur Entkopplung von UI-Schichten verwenden.
 * - Generische Effect-Infrastruktur aus Shared_01 wiederverwenden.
 */
