package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   snackbarHostState: SnackbarHostState,
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
   var enableSave by remember { mutableStateOf(false) }
   enableSave = person.firstName.isNotEmpty() && person.lastName.isNotEmpty()

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
               IconButton(onClick = {
                  if(enableSave)
                     viewModel.onIntent(PersonIntent.Save)
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_back))
               }
            },
            title = {
               Text(text = stringResource( if (personUiState.isNew) R.string.person_create
               else R.string.person_detail ))
            }
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState,
            modifier = Modifier.imePadding())
      },
   ) { innerPadding ->
      // Show a loading indicator
      if (personUiState.isLoading) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      // Show the PersonScreen if the person data is loaded.
      } else {
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

            onSave = { viewModel.onIntent(PersonIntent.Save) },
            onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

            modifier = Modifier
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
