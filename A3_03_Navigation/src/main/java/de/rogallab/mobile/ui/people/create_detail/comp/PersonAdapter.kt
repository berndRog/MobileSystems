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
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonLoadFailure
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

   // Person data
   val person = personUiState.person
   val loadFailure = personUiState.loadFailure

   // Collect one-time effects and translate them into UI callbacks.
   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            navigationIcon = {
               IconButton(enabled = !personUiState.isSaving && !personUiState.isLoading, onClick = {
                  viewModel.onIntent(if (loadFailure == null) PersonIntent.Save else PersonIntent.Cancel)
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                       contentDescription = stringResource(
                          if (loadFailure == null) R.string.action_save
                          else R.string.action_back
                       ))
               }
            },
            title = {
               Text(text = stringResource( if (personUiState.isNew) R.string.person_create
               else R.string.person_detail ))
            }
         )
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
            Button(
               onClick = {
                  viewModel.onIntent(
                     if (loadFailure is PersonLoadFailure.NotFound) PersonIntent.Cancel
                     else PersonIntent.RetryLoad
                  )
               },
            ) {
               Text(text = stringResource(
                  if (loadFailure is PersonLoadFailure.NotFound) R.string.action_back
                  else R.string.action_retry
               ))
            }
         }
      } else {

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
               .imePadding()
         )
      }
   }
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
 * - Im Formular erzeugen Toolbar-Pfeil und Save-Button PersonIntent.Save;
 *   der Cancel-Button erzeugt PersonIntent.Cancel. Bei einem Ladefehler führt
 *   der Toolbar-Pfeil zurück. Der Fehlerzustand bietet je nach Ursache einen
 *   Rückweg oder Retry. Während Save läuft, bleiben die Aktionen gesperrt.
 *
 * Lernziele:
 *
 * - State, Intent und Effect in einem Adapter verbinden.
 * - Meldungen und Navigation als getrennte Effect-Arten behandeln.
 */
